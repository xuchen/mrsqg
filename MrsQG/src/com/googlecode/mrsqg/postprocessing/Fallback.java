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
	protected static Logger log = Logger.getLogger(Fallback.class);
	protected Cheap parser;
	protected LKB generator;

	protected ArrayList<Pair> genSuccPairs;
	protected ArrayList<Pair> genFailPairs;
	protected ArrayList<Pair> oriPairs;

	public Fallback (Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		this.parser = cheap;
		this.generator = lkb;
		this.oriPairs = oriPairs;
		this.genSuccPairs = new ArrayList<Pair>();
		this.genFailPairs = new ArrayList<Pair>();
	}
	
	public ArrayList<Pair> getGenSuccPairs () {return genSuccPairs;}
	public ArrayList<Pair> getGenFailPairs () {return genFailPairs;}

	public void doIt () {
		Preprocessor pre = new Preprocessor();

		if (oriPairs == null) return;
		
		log.info("============== Fallback Generation ==============");
		String sentence;
		
		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			
			Term[] terms = pre.getTerms()[0];
			//sentence = pre.getSentences()[0];

			if (terms == null) continue;

			for (Term term:terms) {
				for (String neType:term.getNeTypes()) {
					if (neType.length()==0) {
						log.error("NE types shouldn't be none: "+term);
						continue;
					}
					String tranSent, tranSent1=null, extraTranSent=null;
					String sentType, extraSentType=null;
					if (neType.equals("NEperson")||neType.equals("NEfirstName")) {
						tranSent = Fallback.transformSentence(sentence, term, "who");
						if (term.getPosFSC().endsWith("S")) {
							tranSent1 = "Who are "+term.getText()+"?";
						} else {
							tranSent1 = "Who is "+term.getText()+"?";
						}
						sentType = "WHO";
					} else if (neType.equals("NElocation")) {
						tranSent = Fallback.transformSentence(sentence, term, "where");
						sentType = "WHERE";
						if (term.getPosFSC().endsWith("S")) {
							tranSent1 = "Where are "+term.getText()+"?";
						} else {
							tranSent1 = "Where is "+term.getText()+"?";
						}
					} else if (neType.equals("NEdate")||neType.equals("NEtime")) {
						tranSent = Fallback.transformSentence(sentence, term, "when");
						sentType = "WHEN";
					} else if (neType.equals("NEnumber")||neType.equals("NEhour")) {
						tranSent = Fallback.transformSentence(sentence, term, "how many");
						sentType = "HOW MANY";
						extraTranSent = Fallback.transformSentence(sentence, term, "how much");
						extraSentType = "HOW MUCH";
					} else if (neType.equals("NEcountry")) {
						tranSent = Fallback.transformSentence(sentence, term, "which country");
						sentType = "WHICH";
					} else {
						tranSent = Fallback.transformSentence(sentence, term, "what");
						sentType = "WHAT";
					}
					
					generate(tranSent, sentType, "Fallback");
					if (tranSent1!=null)
						generate(tranSent1, sentType, "Fallback");
					if (extraTranSent!=null)
						generate(extraTranSent, extraSentType, "Fallback");
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
		String text = term.getText();
		int cfrom = term.getCfrom(), cto = term.getCfrom();
		if (cfrom > sentence.length() ||cto > sentence.length() )
			return null;
		if (text.equals((sentence.substring(cfrom, cto)))) {
			ret =  sentence.substring(0, cfrom) + quesWord + sentence.substring(cto);
		} else if (sentence.contains(text)){
			ret = sentence.replaceAll(text, quesWord);
		} else {
			ret =  sentence.substring(0, cfrom) + quesWord + sentence.substring(cto);
		}
		if (ret.substring(ret.length()-1).equals("."))
			ret = ret.substring(0, ret.length()-1) + "?";
		else ret = ret + "?";
		return ret;
	}
	
	protected void generate (String tranSent, String sentType, String source) {
		if (tranSent == null) return;
		Preprocessor pre = new Preprocessor();
		
		String fsc = pre.getFSCbyTerms(tranSent, true);
		log.info("Fallback sentence:");
		log.info(tranSent);

		parser.parse(fsc);
		ArrayList<MRS> mrxList = parser.getParsedMRSlist();
		boolean success = parser.isSuccess();

		if (pre.getNumTokens() > 15) {
			parser.releaseMemory();
		}
		if (!success) {
			Pair pair = new Pair (tranSent, sentType);
			genFailPairs.add(pair);
			return;
		}

		if (mrxList != null && this.generator != null) {
			String mrx;
			for (MRS m:mrxList) {
				// generate from original sentence
				m.changeFromUnkToNamed();
				mrx = m.toMRXstring();
				m.setSentType(sentType);
				generator.sendMrxToGen(mrx);
				log.info("\nGenerate from fallback sentence:\n");

				ArrayList<String> genQuesList = generator.getGenSentences();
				ArrayList<String> genQuesFailedList = null;
				log.info(genQuesList);
				
				// Add to pair list
				if (!(genQuesList==null && genQuesFailedList==null)) {
					m.setDecomposer(source);
					Pair pair = new Pair (m, genQuesList, genQuesFailedList);
					pair.setTranSent(tranSent);
					if (genQuesList!=null)	genSuccPairs.add(pair);
					else genFailPairs.add(pair);
				}
			}
		}

	}
	
	/**
	 * Change the punctuation in the last to ?
	 */
	protected String changeQuestionMark (String tranSent) {
		if (tranSent.substring(tranSent.length()-1).equals("."))
			tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
		else tranSent = tranSent + "?";
		return tranSent;
	}

}
