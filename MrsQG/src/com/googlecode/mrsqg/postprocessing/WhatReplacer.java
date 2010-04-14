/**
 * 
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;
import java.util.HashSet;

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
public class WhatReplacer extends Fallback {

	public WhatReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}
	
	public void doIt() {
			
			if (this.oriPairs == null) return;
			
			Preprocessor pre = new Preprocessor();
			String sentence;
			String tranSent;
			
			String beEPvalue = "_BE_V_ID_REL";
			
			log.info("============== MrsReplacer Generation -- WhatReplacer==============");
	
			for (Pair oriPair:oriPairs) {
				if (oriPair.getGenOriCand()!=null) {
					pre.preprocess(oriPair.getGenOriCand());
				} else {
					pre.preprocess(oriPair.getOriSent());
				}
				
				//sentence = pre.getSentences()[0];
				sentence = oriPair.getOriSent();
				MRS mrs = oriPair.getOriMrs();
				
				for (ElementaryPredication ep:mrs.getEps()) {
					if (ep.getTypeName().equals(beEPvalue)) {
						int cfrom = ep.getCfrom();
						int cto = ep.getCto();
						
						if (cfrom > sentence.length() || cto > sentence.length()) continue;
						
						tranSent = "what " + sentence.substring(cfrom);
						tranSent = changeQuestionMark(tranSent);					
						generate(tranSent, "WHAT", "WhatReplacer");
						
						tranSent = sentence.substring(0, cto) + " what ?";
						generate(tranSent, "WHAT", "WhatReplacer");
						
					}
				}
			}
		}

}
