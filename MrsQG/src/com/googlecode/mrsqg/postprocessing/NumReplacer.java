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
		
		String beEPvalue = "CARD_REL";
		
		log.info("============== MrsReplacer Generation -- NumReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()==null) continue;
			pre.preprocess(oriPair.getGenOriCand());
			
			//sentence = pre.getSentences()[0];
			sentence = oriPair.getOriSent();
			MRS mrs = oriPair.getOriMrs();
			
			ElementaryPredication oldEP = null;
			
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(beEPvalue)) {
					if (oldEP != null && oldEP.getTypeName().equals(beEPvalue)) continue;
					int cfrom = ep.getCfrom();
					int cto = ep.getCto();
					
					if (cfrom > sentence.length() || cto > sentence.length()) continue;
					
					tranSent = sentence.substring(0, cfrom) + "how many ?";					
					generate(tranSent, "HOW MANY/MUCH", "NumReplacer");
					
					tranSent = sentence.substring(0, cfrom) + "how much ?";					
					generate(tranSent, "HOW MANY/MUCH", "NumReplacer");
					oldEP = ep;
				}
			}
		}
	}
}
