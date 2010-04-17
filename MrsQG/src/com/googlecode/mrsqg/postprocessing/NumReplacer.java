/**
 * 
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;

/**
 * @author Xuchen Yao
 *
 */
public class NumReplacer extends Fallback {

	public NumReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}

	public void doIt() {
		
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;
		String tranSent1;
		
		String beEPvalue = "CARD_REL";
		
		log.info("============== MrsReplacer Generation -- NumReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			MRS mrs = oriPair.getOriMrs();
			
			ElementaryPredication oldEP = null;
			
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(beEPvalue)) {
					if (oldEP != null && oldEP.getTypeName().equals(beEPvalue)) continue;
					int cfrom = ep.getCfrom();
					int cto = ep.getCto();
					
					if (cfrom > sentence.length() || cto > sentence.length()) continue;
					
					String carg = ep.getValueByFeature("CARG");
					if (carg!=null && carg.equals((sentence.substring(cfrom, cto)))) {
						tranSent = sentence.substring(0, cfrom) + "how many" + sentence.substring(cto);
						tranSent1 = sentence.substring(0, cfrom) + "how much" + sentence.substring(cto);
					} else if (carg!=null && sentence.contains(carg)) {
						tranSent = sentence.replaceAll(carg, "how many");
						tranSent1 = sentence.replaceAll(carg, "how much");
					} else {
						tranSent = sentence.substring(0, cfrom) + "how many" + sentence.substring(cto);
						tranSent1 = sentence.substring(0, cfrom) + "how much" + sentence.substring(cto);
					}
					
					tranSent = changeQuestionMark(tranSent);
					tranSent1 = changeQuestionMark(tranSent1);
					
					generate(tranSent, "HOW MANY/MUCH", "NumReplacer");
									
					generate(tranSent1, "HOW MANY/MUCH", "NumReplacer");
					oldEP = ep;
				}
			}
		}
	}
}
