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
			
			String verbEPvalue = "_V_";
			
			log.info("============== MrsReplacer Generation -- WhatReplacer==============");
	
			for (Pair oriPair:oriPairs) {
				if (oriPair.getGenOriCand()!=null) {
					sentence = oriPair.getGenOriCand();
				} else {
					sentence = oriPair.getOriSent();
				}
				
				pre.preprocess(sentence);
				MRS mrs = oriPair.getOriMrs();
				
				for (ElementaryPredication ep:mrs.getEps()) {
					if (ep.getTypeName().toUpperCase().contains(verbEPvalue)) {
						int cfrom = ep.getCfrom();
						int cto = ep.getCto();
						
						if (cfrom > sentence.length() || cto > sentence.length() ||
								cfrom < 0 || cto < 0) continue;
						
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
