package com.googlecode.mrsqg.analysis;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.mrsqg.analysis.TruncationFilter;
import com.googlecode.mrsqg.nlp.NETagger;
import com.googlecode.mrsqg.nlp.OpenNLP;
import com.googlecode.mrsqg.util.Dictionary;
import com.googlecode.mrsqg.util.StringUtils;

/**
 * Extracts single- and multi-token terms from a sentence. Multi-token terms are
 * named entities or compound terms found in dictionaries.
 *
 * @author Nico Schlaefer
 * @version 2007-05-28
 */
public class TermExtractor {
	/** Maximum length of a term in tokens. */
	private static final int MAX_TERM_LENGTH = 4;

	/**
	 * Checks if the given term is among the named entities and returns the
	 * types of the entities that match it.
	 *
	 * @param term a term, potentially a named entity
	 * @param nes named entities
	 * @return types of matching entities
	 */
	// BUG fix. when a term such as "Gary, Indiana" (all single words are NElocation) comes,
	// it will recognize it as a term and return NElocation.
	private static String[] getNeTypes(String term, String[][] nes) {
		List<String> neTypes = new ArrayList<String>();
		Set<String> neTypesSet = new HashSet<String>();
		int stanfordStart = NETagger.getStanfordStart();

		for (int neId = 0; neId < nes.length; neId++)
			for (String ne : nes[neId])
				if (term.equals(ne)) {
					String neType = NETagger.getNeType(neId);
					if (neTypesSet.add(neType))
						// there may be multiple taggers (IDs) for one type
						neTypes.add(neType);
					break;
				}

		// NEs for stanford tagger. let's try again
		// remove all punctuation since Stanford tagger doesn't contain any
		String[] termNoPunc = term.replaceAll("\\p{Punct}+", "").split("\\s+");
		for (int neId = stanfordStart; neId < nes.length; neId++) {
			boolean contain = false;
			ArrayList<String> nesList = new ArrayList<String>(Arrays.asList(nes[neId]));
			// check if every term in termNoPunc has the same NE type
			for (String t: termNoPunc) {
				if (nesList.contains(t)) {
						contain = true;
				} else {
					contain = false;
					break;
				}
			}
			if (contain) {
				String neType = NETagger.getNeType(neId);
				if (neTypesSet.add(neType))
					// there may be multiple taggers (IDs) for one type
					neTypes.add(neType);
			}
		}

		// disambiguate with the priority of stanford NE tagger
		// if Jackson is a person, then the following removes NEprovince, NEcapital from neTypes
		if (neTypes.contains("NEperson")) {
			String[] persons = {"NEactor", "NEauthor", "NEdirector", "NEfirstName", "NEscientist", "NEusPresident", "NEperson"};
			ArrayList<String> list = new ArrayList<String>(Arrays.asList(persons));
			String[] types = neTypes.toArray(new String[neTypes.size()]);
			for (String type:types) {
				if (!list.contains(type)) {
					neTypes.remove(type);
				}
			}
		}
		return neTypes.toArray(new String[neTypes.size()]);
	}

	/**
	 * Extracts named entities from the given sentence.
	 *
	 * @param sentence sentence to analyze
	 * @return named entities in the sentence
	 */
	public static String[][] getNes(String sentence) {
		String[] tokens = NETagger.tokenize(sentence);
		String[][] nes = NETagger.extractNes(new String[][] {tokens})[0];

		// untokenize named entities
		for (int i = 0; i < nes.length; i++)
			for (int j = 0; j < nes[i].length; j++)
				nes[i][j] = OpenNLP.untokenize(nes[i][j], sentence);

		return nes;
	}

	/**
	 * Extracts named entities from the given sentence and context string.
	 *
	 * @param sentence sentence to analyze
	 * @param context context string
	 * @return named entities in the sentence and context string
	 */
	public static String[][] getNes(String sentence, String context) {
		// extract NEs from sentence
		String[][] sentenceNes = getNes(sentence);
		if (context == null || context.length() == 0) return sentenceNes;

		// extract NEs from context string
		String[][] contextNes = getNes(context);

		// merge NEs
		String[][] nes = new String[sentenceNes.length][];
		for (int i = 0; i < nes.length; i++) {
			if (sentenceNes[i].length == 0) nes[i] = contextNes[i];
			else if (contextNes[i].length == 0) nes[i] = sentenceNes[i];
			else {
				ArrayList<String> nesL = new ArrayList<String>();
				for (String ne : sentenceNes[i]) nesL.add(ne);
				for (String ne : contextNes[i]) nesL.add(ne);
				nes[i] = nesL.toArray(new String[nesL.size()]);
			}
		}
		return nes;
	}

	/**
	 * Extracts terms from the given sentence.
	 *
	 * @param sentence sentence to analyze
	 * @param dicts dictionaries with compound terms
	 * @return terms in the sentence
	 */
	public static Term[] getTerms(String sentence, Dictionary[] dicts) {
		String[][] nes = getNes(sentence);

		return getTerms(sentence, nes, dicts);
	}

	/**
	 * Extracts terms from the given sentence, reusing named entities that have
	 * been extracted before.
	 *
	 * @param sentence sentence to analyze
	 * @param nes named entities in the sentence
	 * @param dicts dictionaries with compound terms
	 * @return terms in the sentence
	 */
	public static Term[] getTerms(String sentence, String[][] nes,
			Dictionary[] dicts) {
		// extract tokens
		String[] tokens = OpenNLP.tokenize(sentence);
		// the start position (in characters) of each token
		int[] tokenStart = new int[tokens.length];
		for (int i=0, offset=0; i<tokens.length; i++) {
			tokenStart[i] = offset;
			// 1 for a space
			offset += (tokens[i].length()+1);
		}
		// tag part of speech
		String[] pos = OpenNLP.tagPos(tokens);
		// temporarily avoid errors such as invalid predicates: |"_thermoplastics_nns_rel"|
		// X. Yao 2010-05-16: Disable it to use the new LKB/logon with generation from unknown words.
//		for (int j=0; j<pos.length; j++) {
//			if (pos[j].equals("NNS")) pos[j] = "NNPS";
//		}
		// tag phrase chunks
		String[] chunks = OpenNLP.tagChunks(tokens, pos);
		// mark tokens as not yet assigned to a term
		boolean[] assigned = new boolean[tokens.length];
		Arrays.fill(assigned, false);

		List<Term> termsL = new ArrayList<Term>();

		// normalized terms (do identify duplicates)
		Set<String> termSet = new HashSet<String>();

		// construct multi-token terms
		for (int length = MAX_TERM_LENGTH; length > 1; length--)
			// X. YAO, 2010-3-3.
			// in LKB/ERG parsing, every sentence must be ended with a punctuation mark.
			// However, in "John likes Mary." "Mary." would be recognized as a NEperson
			// since the Stanford NER doesn't care punctuation. So the last punctuation
			// mark is not selected as a term candidate here to avoid "Mary.".
			for (int id = 0; id < tokens.length - length; id++) {
			//for (int id = 0; id < tokens.length - length + 1; id++) {
				// one of the tokens is already assigned to a term?
				boolean skip = false;
				for (int offset = 0; offset < length; offset++)
					if (assigned[id + offset]) {
						skip = true;
						continue;
					}
				if (skip) continue;

				/*
				 * X. Yao. 2010-08-13. Comma in ERG seems to be important.
				 * e.g. "in Germany , Austria , Switzerland , and Slovakia , beer is usually made from just hops, malt, water, and yeast."
				 * if "Germany , Austria ," or "Slovakia ," is recognized as NElocation, ERG sometimes doesn't parse
				 * so we rule out any NEs that start or end with a comma.
				 */
				if (pos[id].equals(",")) continue;
				if (pos[id+length-1].equals(",")) continue;

				// get phrase spanning the tokens
				String text = tokens[id];
				String untokText;
				for (int offset = 1; offset < length; offset++)
					text += " " + tokens[id + offset];
				untokText = text;
				text = OpenNLP.untokenize(text, sentence);

				// phrase is a duplicate?
				if (!termSet.add(StringUtils.normalize(text))) continue;
				// phrase does not contain keywords?
				if (KeywordExtractor.getKeywords(text).length == 0) continue;

				// phrase is a named entity?
				// BUG fix: the RegEx tagger will recognize a NEdate, such as "August 29, 1958",
				// as "August 29 , 1958", thus all tokens in the text should be concatenated with space
				String[] neTypes = getNeTypes(StringUtils.concatWithSpaces(OpenNLP.tokenize(text)), nes);
				if (neTypes.length > 0) {
					// construct term
					Term t = new Term(untokText, Term.COMPOUND, neTypes, id, id+length, tokenStart);
					t.setPosFSC(pos);
					termsL.add(t);
					// mark tokens as assigned
					for (int offset = 0; offset < length; offset++)
						assigned[id + offset] = true;
					continue;
				}

				for (Dictionary dict : dicts) {
					// phrase is not a noun phrase or verb phrase?
					if (!(chunks[id].endsWith("NP") &&  // look up noun phrases
							chunks[id + length - 1].endsWith("NP"))/* &&
						!(chunks[id].endsWith("VP") &&  // look up verb phrases
							chunks[id + length - 1].endsWith("VP"))*/)
						continue;

					// phrase contains a special characters other than '.'?
					if (text.matches(".*?[^\\w\\s\\.].*+")) continue;
					// phrase can be truncated?
					if (!text.equals(TruncationFilter.truncate(text))) continue;

					// phrase is in the dictionary?
					if (dict.contains(text)) {
						// construct term
						Term t = new Term(untokText, Term.COMPOUND, neTypes, id, id+length, tokenStart);
						t.setPosFSC(pos);
						termsL.add(t);
						// mark tokens as assigned
						for (int offset = 0; offset < length; offset++)
							assigned[id + offset] = true;
						continue;
					}
				}
			}

		// construct single-token terms
		for (int id = 0; id < tokens.length; id++) {
			// token is part of a multi-token term?
			//X. Yao. Aug 13, 2009. Different NEtaggers might conflict.
			//for instance, "red kangaroo" is NEorganization by Stanford NEtagger
			//"kangaroo" is NEanimal by list tagger
			//thus comment out the following line to have more NEs.
			// X. Yao. Mar 2, 2010. Uncomment to gain less NEs for FSC output.
			if (assigned[id]) continue;

			// token is a duplicate?
			if (!termSet.add(StringUtils.normalize(tokens[id]))) continue;
			// token does not contain keywords?
			if (KeywordExtractor.getKeywords(tokens[id]).length == 0) continue;

			// get named entity types and construct term
			String[] neTypes = getNeTypes(tokens[id], nes);
			Term t = new Term(tokens[id], pos[id], neTypes, id, id+1, tokenStart);
			t.setPosFSC(pos);
			termsL.add(t);
		}

		return termsL.toArray(new Term[termsL.size()]);
	}

	/**
	 * Extracts terms from the given sentence and context string.
	 *
	 * @param sentence sentence to analyze
	 * @param context context string
	 * @param nes named entities in the sentence and context string
	 * @param dicts dictionaries with compound terms
	 * @return terms in the sentence and context string
	 */
	public static Term[] getTerms(String sentence, String context,
			String[][] nes, Dictionary[] dicts) {
		// extract terms from sentence
		Term[] sentenceTerms = getTerms(sentence, nes, dicts);
		if (context == null || context.length() == 0) return sentenceTerms;

		// extract terms from context string
		Term[] contextTerms = getTerms(context, nes, dicts);
		if (sentenceTerms.length == 0) return contextTerms;
		if (contextTerms.length == 0) return sentenceTerms;

		// merge terms, eliminate duplicates
		List<Term> terms = new ArrayList<Term>();
		Set<String> termSet = new HashSet<String>();
		for (Term sentenceTerm : sentenceTerms)
			if (termSet.add(StringUtils.normalize(sentenceTerm.getText())))
				terms.add(sentenceTerm);
		for (Term contextTerm : contextTerms)
			if (termSet.add(StringUtils.normalize(contextTerm.getText())))
				terms.add(contextTerm);
		return terms.toArray(new Term[terms.size()]);
	}

	/**
	 * Extracts single-token terms from the given sentence.
	 *
	 * @param sentence sentence to analyze
	 * @return single-token terms in the sentence
	 */
	public static Term[] getSingleTokenTerms(String sentence) {
		// extract tokens
		String[] tokens = OpenNLP.tokenize(sentence);
		// tag part of speech
		String[] pos = OpenNLP.tagPos(tokens);

		// extracted terms
		ArrayList<Term> terms = new ArrayList<Term>();
		// normalized terms (do identify duplicates)
		Set<String> termSet = new HashSet<String>();

		// construct single-token terms
		for (int id = 0; id < tokens.length; id++) {
			// token is a duplicate?
			if (!termSet.add(StringUtils.normalize(tokens[id]))) continue;
			// token does not contain keywords?
			if (KeywordExtractor.getKeywords(tokens[id]).length == 0) continue;

			// construct term
			terms.add(new Term(tokens[id], pos[id]));
		}

		return terms.toArray(new Term[terms.size()]);
	}
}
