/**
 * This is "Plan B" of MrsQG, employing a fallback strategy of re-generation
 * by re-parsing. That is, if MrsQG failed to generate from MrsTransformer, 
 * then the question word will replace the term in a sentence, and be sent to
 * the parser, finally the parsing result will be sent to the generator for a
 * more natural re-organization of the sentence. A rough example of pipelines:
 * <p>
 * "He has 5 apples." -> "He has how many apples?" -> send to parser 
 * -> get MRS from parser -> send to the generator -> "How many apples does he have?"
 * <p>
 *  However, plan B doesn't always work that well. So needing plan C...
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;
import com.googlecode.mrsqg.analysis.Pair;

/**
 * @author Xuchen Yao
 * @since 2010-04-03
 *
 */
public class Fallback {
	private static Logger log = Logger.getLogger(Fallback.class);
	private Cheap parser;
	private LKB generator;
	private Preprocessor pre;
	
	protected ArrayList<Pair> pairs;
	
	public Fallback (Cheap cheap, LKB lkb, Preprocessor pre) {
		this.parser = cheap;
		this.generator = lkb;
		this.pre = pre;
		this.pairs = new ArrayList<Pair>();
	}
	
	public void doIt () {
		Term[] terms = pre.getTerms()[0];
		String sentence = pre.getSentences()[0];
		
		if (terms == null) return;
		Pair pair;
		Pair extraPair = null;
		
		for (Term term:terms) {
			for (String neType:term.getNeTypes()) {
				if (neType.length()==0) {
					log.error("NE types shouldn't be none: "+term);
					continue;
				}
				String tranSent;
				extraPair = null;
				if (neType.equals("NEperson")||neType.equals("NEfirstName")) {
					tranSent = Fallback.transformSentence(sentence, term, "who");
					pair = new Pair(sentence, tranSent, "WHO");
				} else if (neType.equals("NElocation")) {
					tranSent = Fallback.transformSentence(sentence, term, "where");
					pair = new Pair(sentence, tranSent, "WHERE");
				} else if (neType.equals("NEdate")||neType.equals("NEtime")) {
					tranSent = Fallback.transformSentence(sentence, term, "when");
					pair = new Pair(sentence, tranSent, "WHEN");
				} else if (neType.equals("NEnumber")||neType.equals("NEhour")) {
					tranSent = Fallback.transformSentence(sentence, term, "how many");
					pair = new Pair(sentence, tranSent, "HOW MANY");
					tranSent = Fallback.transformSentence(sentence, term, "how much");
					extraPair = new Pair(sentence, tranSent, "HOW MUCH");
				} else if (neType.equals("NEcountry")) {
					tranSent = Fallback.transformSentence(sentence, term, "which country");
					pair = new Pair(sentence, tranSent, "WHICH");
				} else {
					tranSent = Fallback.transformSentence(sentence, term, "what");
					pair = new Pair(sentence, tranSent, "WHAT");
				}
				
				pairs.add(pair);
				if (extraPair != null) {
					pairs.add(extraPair);
				}
			}
		}
		
		log.info("============== Fallback Generation ==============");
		
		Preprocessor pre;
		for (Pair p:this.pairs) {
			pre = new Preprocessor();
			String fsc = pre.getFSCbyTerms(p.getTranSent(), true);
			log.info("Transformed sentence:");
			log.info(p.getTranSent());
//			log.info("\nFSC XML from preprocessing:\n");
//			log.info(fsc);
			parser.parse(fsc);
			ArrayList<MRS> mrxList = parser.getParsedMRSlist();
			p.setTranMrs(mrxList);
			if (pre.getNumTokens() > 15) {
				parser.releaseMemory();
			}
			if (!parser.isSuccess()) continue;
			// TODO: add MRS selection here

			if (mrxList != null && this.generator != null) {
				String mrx;
				for (MRS m:mrxList) {
					// generate from original sentence
					m.changeFromUnkToNamed();
					mrx = m.toMRXstring();
					generator.sendMrxToGen(mrx);
					log.info("\nGenerate from transformed sentence:\n");
					ArrayList<String> genSents = generator.getGenSentences();
					log.info(genSents);
//					log.info("\nFrom the following MRS:\n");
//					log.info(mrx);
//					log.info(m);
					if (genSents != null)
						p.getGenSentList().addAll(genSents);
				}
			}
		}
	}
	
	/**
	 * Replace the term in a sentence with <code>quesWord</code>. For instance,
	 * "John likes Mary." -> "John likes who?"
	 * @param sentence the original sentence
	 * @param term the term to be replaced
	 * @param quesWord the question word
	 * @return a replaced string
	 */
	public static String transformSentence (String sentence, Term term, String quesWord) {
		String ret;
		ret =  sentence.substring(0, term.getCfrom()) + quesWord + sentence.subSequence(term.getCto(), sentence.length());
		if (ret.substring(ret.length()-1).equals("."))
			ret = ret.substring(0, ret.length()-1) + "?";
		else ret = ret + "?";
		return ret;
	}

}
