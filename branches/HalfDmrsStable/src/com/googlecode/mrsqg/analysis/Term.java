package com.googlecode.mrsqg.analysis;


import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.nlp.indices.FunctionWords;
import com.googlecode.mrsqg.nlp.semantics.ontologies.WordNet;
import com.googlecode.mrsqg.util.StringUtils;

import net.didion.jwnl.data.POS;

//import net.didion.jwnl.data.POS;

/**
 * <p>A <code>Term</code> comprises one or more tokens of text that form a unit
 * of meaning. It can be an individual word, a compound noun or a named entity.
 * </p>
 *
 * <p>This class implements the interface <code>Serializable</code>.</p>
 *
 * @author Nico Schlaefer
 * @version 2008-01-23
 *
 * X. Yao: Add more class members to support FSC output
 */
public class Term implements Serializable {

	private static Logger log = Logger.getLogger(Term.class);

	/** Version number used during deserialization. */
	private static final long serialVersionUID = 20070501;

	/** Part of speech tag for terms that comprise multiple tokens */
	public static final String COMPOUND = "COMPOUND";

	/** The textual representation of the term. */
	private String text;

	/**
	 * X. YAO. 2010-3-3.
	 * position in a sentence, such as "Al Gore" has
	 * from = 0, to = 2 in sentence "Al Gore was born in Washington DC."*/
	private int from;
	private int to;
	/**
	 * X. YAO. 2010-3-3.
	 * position (in characters) in a sentence, such as "Al Gore" has
	 * cfrom = 0, cto = 7 in sentence "Al Gore was born in Washington DC."*/
	private int cfrom;
	private int cto;
	/** The lemma of the term. */
	private String lemma;
	/**
	 * The part of speech of the term or <code>COMPOUND</code> to indicate that
	 * it comprises multiple tokens.
	 */
	private String pos;
	/**
	 * POS for the FSC format. e.g. "Al(NNP) Gore(NNP)" is an NP but FSC doesn't support this.
	 * Thus the pos_fsc is set to NNP for this term.
	 */
	private String pos_fsc;
	/** The named entity types of the term (optional). */
	private String[] neTypes = new String[0];
	/** Relative frequency of the term. */
	private double relFrequency;
	/** Maps expansions of the term to their weights. */
	private Map<String, Double> expansions;
	/** Maps lemmas of the expansions to their weights. */
	private Map<String, Double> expansionLemmas;

	// Getters/Setters
	public String getText() {return text;}
	public int getFrom() {return from;}
	public int getTo() {return to;}
	public int getCfrom() {return cfrom;}
	public int getCto() {return cto;}
	public String getLemma() {return lemma;}
	public String getPos() {return pos;}
	public String getPosFSC() {return pos_fsc;}
	public String[] getNeTypes() {return neTypes;}
	public void setNeTypes(String[] neTypes) {this.neTypes = neTypes;}
	public double getRelFrequency() {return relFrequency;}
	public void setRelFrequency(double relFrequency) {
		this.relFrequency = relFrequency;}
	public Map<String, Double> getExpansions() {return expansions;}
	public void setExpansions(Map<String, Double> expansions) {
		this.expansions = expansions;}

	/**
	 * Constructs a term from the provided information.
	 *
	 * @param text textual representation
	 * @param pos part of speech
	 */
	public Term(String text, String pos) {
		this.text = text;
		this.pos = pos;

		// derive the lemma
		// warning: this could be extremely slow when text is a long string
		generateLemma();
	}

	/**
	 * Constructs a term from the provided information.
	 *
	 * @param text textual representation
	 * @param pos part of speech
	 * @param neTypes named entity types
	 */
	public Term(String text, String pos, String[] neTypes) {
		this(text, pos);
		this.neTypes = neTypes;
	}

	public Term(String text, String pos, String[] neTypes, int from, int to, int[] tokenStart) {
		this(text, pos);
		this.neTypes = neTypes;
		this.from = from;
		this.to = to;
		this.cfrom = tokenStart[from];
		this.cto = this.cfrom+text.length();

		if (to==tokenStart.length-1) {
			// this term spans the last word
			// in accordance with MRX, add the final space and the punctuation mark.
			// e.g. in the MRS output of "John likes Mary ." "Mary" is from
			// 11 to 17, spanning the last space and full stop. Thus cto should plus 2

			this.cto+=2;
		}
	}

	public Term(String text, String pos, String[] neTypes, String lemma) {
		this.text = text;
		this.pos = pos;
		this.neTypes = neTypes;
		this.lemma= lemma;
	}

	/**
	 * Construct a term from another term but set neTypes with an extra neTypes
	 */
	public Term(Term term, String[] neTypes) {
		this.text = term.getText();
		this.pos = term.getPos();
		this.lemma = term.getLemma();
		this.neTypes = neTypes;
	}
	/**
	 * Generates the lemma of the term.
	 */
	private void generateLemma() {
		String lemma;
		if (pos.startsWith("VB")) {
			// lemmatize verbs that are in WordNet
			lemma = WordNet.getLemma(text, POS.VERB);
		} else if (pos.startsWith("JJ")) {
			// lemmatize adjectives that are in WordNet
			lemma = WordNet.getLemma(text, POS.ADJECTIVE);
		} else if (pos.startsWith("RB")) {
			// lemmatize adverbs that are in WordNet
			lemma = WordNet.getLemma(text, POS.ADVERB);
		} else {
			// lemmatize nouns that are in WordNet
			if (pos.startsWith("COMPOUND"))
				lemma = WordNet.getCompoundLemma(text, POS.NOUN);  // compound
			else
				lemma = WordNet.getLemma(text, POS.NOUN);  // single token
		}
		if (lemma == null) lemma = text;

		setLemma(lemma);
	}

	/**
	 * Normalizes and sets the lemma of the term.
	 *
	 * @param lemma the lemma of the term
	 */
	public void setLemma(String lemma) {
		this.lemma = StringUtils.normalize(lemma);
	}

	/**
	 * Normalizes and sets the lemmas of the expansions.
	 *
	 * @param expansionLemmas the lemmas of the expansions
	 */
	public void setExpansionLemmas(Map<String, Double> expansionLemmas) {
		Map<String, Double> normalized = new Hashtable<String, Double>();

		for (String lemma : expansionLemmas.keySet()) {
			double weight = expansionLemmas.get(lemma);
			String norm = StringUtils.normalize(lemma);
			normalized.put(norm, weight);
		}

		this.expansionLemmas = normalized;
	}

	/**
	 * Gets the weight of the term or expansion with the given lemma.
	 *
	 * @param lemma the lemma
	 * @return the weight or <code>0</code> if there is no match
	 */
	public double getWeight(String lemma) {
		if (lemma.equals(this.lemma)) return 1;

		if (expansionLemmas == null) return 0;
		Double weight = expansionLemmas.get(lemma);
		return (weight != null) ? weight : 0;
	}

	/**
	 * Calculates similarity scores for the given lemma and the lemmas of the
	 * term and its expansions based on their weights and the number of common
	 * tokens. Gets the maximum of all these scores.
	 *
	 * @param lemma lemma to compare with
	 * @return similarity score
	 */
	public double simScore(String lemma) {
		// tokenize lemma,
		// eliminate duplicates, function words and tokens of length < 2
		String[] tokens = lemma.split(" ");
		Set<String> lookupSet = new HashSet<String>();
		for (String token : tokens)
			if (token.length() > 1 && !FunctionWords.lookup(token))
				lookupSet.add(token);
		if (lookupSet.size() == 0) return 0;

		// calculate similarity score for the term
		// (Jaccard coefficient)
		tokens = this.lemma.split(" ");
		Set<String> tokenSet = new HashSet<String>();
		for (String token : tokens)
			if (token.length() > 1 && !FunctionWords.lookup(token))
				tokenSet.add(token);
		double intersect = 0;
		int union = lookupSet.size();
		for (String token : tokenSet)
			if (lookupSet.contains(token)) intersect++; else union++;
		double simScore = intersect / union;

		// calculate similarity scores for the expansions
		// (Jaccard coefficient)
		for (String expansionLemma : expansionLemmas.keySet()) {
			tokens = expansionLemma.split(" ");
			tokenSet.clear();
			for (String token : tokens)
				if (token.length() > 1 && !FunctionWords.lookup(token))
					tokenSet.add(token);
			double weight = expansionLemmas.get(expansionLemma);
			intersect = 0;
			union = lookupSet.size();
			for (String token : tokenSet)
				if (lookupSet.contains(token)) intersect++; else union++;
			simScore = Math.max(simScore, weight * (intersect / union));
		}

		return simScore;
	}

	/**
	 * Set the pos_fsc field of this term.
	 */
	public void setPosFSC(String[] pos) {
		// if this is not a named entity, we'll use this.pos rather than this.pos_fsc
		if (this.neTypes == null) return;
		if (this.from == this.to) {
			this.pos_fsc = pos[this.from];
			return;
		}

		Set<String> posSet = new HashSet<String>();
		for (int i=this.from; i<this.to; i++) {
			posSet.add(pos[i]);
		}

		// All has the same POS. Such as "Al Gore", are all NNP.
		if (posSet.size() == 1) {
			this.pos_fsc = pos[this.from];
			// temporarily avoid errors (NN compound) such as invalid predicates: |"_pie chart_nn_rel"|
			// X. Yao 2010-05-16: Disable it to use the new LKB/logon with generation from unknown words.
//			if (this.pos_fsc.equals("NN"))
//				this.pos_fsc = "NNP";
			return;
		} else {
			// we should have used the CollinsHeadFinder in Stanford parser
			// to find out the POS tag of the head. But there are two issues:
			// 1. the POS returned might not be compatible with the POS from OpenNLP
			// 2. it's not worth using a heavy parser for this simple function
			String[] posL = posSet.toArray(new String[posSet.size()]);
			// Ascending order: NN, NNP, NNPS, NNS
			Arrays.sort(posL);
			this.pos_fsc = posL[posL.length-1];
			// temporarily avoid errors (NN compound) such as invalid predicates: |"_pie chart_nn_rel"|
			// X. Yao 2010-05-16: Disable it to use the new LKB/logon with generation from unknown words.
//			if (this.pos_fsc.equals("NN"))
//				this.pos_fsc = "NNP";
			log.warn("Warning: different POS in term. Using the last one. "+Arrays.toString(posL));
			return;
		}
	}

	/**
	 * Creates a string representation of the term.
	 *
	 * @return string representation
	 */
	public String toString() {
		String s = "{\"" + text + "\"; POS: " + pos;
		if (neTypes.length > 0) {
			s += "; NE types: " + neTypes[0];
			for (int i = 1; i < neTypes.length; i++)
				s += ", " + neTypes[i];
		}
		if (expansions != null &&  expansions.size() > 0) {
			String[] texts =
				expansions.keySet().toArray(new String[expansions.size()]);
			s += "; Expansions: {" + texts[0] + "=" + expansions.get(texts[0]);
			for (int i = 1; i < expansions.size(); i++)
				s += ", " + texts[i] + "=" + expansions.get(texts[i]);
			s += "}";
		}
		s += "}";

		return s;
	}
}
