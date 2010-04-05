/**
 * 
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;

/**
 * @author Xuchen Yao
 * @since 2010-04-05
 */
public class MrsReplacer {
	
	private static Logger log = Logger.getLogger(MrsReplacer.class);
	private Cheap parser;
	private LKB generator;
	private Preprocessor pre;
	private ArrayList<MRS> origList;
	
	protected ArrayList<Pair> pairs;
	
	public MrsReplacer (Cheap cheap, LKB lkb, Preprocessor pre, ArrayList<MRS> list) {
		this.parser = cheap;
		this.generator = lkb;
		this.pre = pre;
		this.origList = list;
		this.pairs = new ArrayList<Pair>();
	}
	
	public void doIt () {
		if (this.origList == null) return;
		
		String sentence = pre.getSentences()[0];
		String tranSent;
		Pair pair;
		
		String andEPvalue = "_AND_C_REL";

		for (MRS mrs:origList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(andEPvalue) && 
						ep.getValueByFeature("L-INDEX") != null &&
						ep.getValueByFeature("R-INDEX") != null &&
						ep.getValueByFeature("L-HNDL") == null &&
						ep.getValueByFeature("R-HNDL") == null) {
					String arg1 = ep.getValueByFeature("L-INDEX");
					String arg2 = ep.getValueByFeature("R-INDEX");
					ArrayList<ElementaryPredication> argList;
					argList = mrs.getEPbyFeatAndValue("ARG0", arg1);
					if (argList == null) continue;
					int cfrom = argList.get(0).getCfrom();
					// find out the left-most index
					for (ElementaryPredication e:argList) {
						if (e.getCfrom()<cfrom) cfrom = e.getCfrom();
					}
					
					argList = mrs.getEPbyFeatAndValue("ARG0", arg2);
					if (argList == null) continue;
					int cto = ep.getCto();
					// find out the right-most index
					for (ElementaryPredication e:argList) {
						if (e.getCto()>cto) cto = e.getCto();
					}
					
					tranSent = sentence.substring(0, cfrom);
					tranSent += "what";
					tranSent += sentence.substring(cto, sentence.length());
					if (tranSent.substring(tranSent.length()-1).equals("."))
						tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
					else tranSent = tranSent + "?";
					
					pair = new Pair(sentence, tranSent, "WHAT");
					pairs.add(pair);
				}
			}
		}
		

		log.info("============== MrsReplacer Generation ==============");
		
		Preprocessor pre;
		for (Pair p:this.pairs) {
			pre = new Preprocessor();
			String fsc = pre.getFSCbyTerms(p.getTranSent());
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

}
