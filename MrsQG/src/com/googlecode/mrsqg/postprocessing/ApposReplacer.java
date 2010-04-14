/**
 * 
pipe: The man after Hurricane Katrina did not cause a big collapse.
-> The man after which Hurricane did not cause a big collapse.

bug:

The girl Anna likes which the dog ?
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
public class ApposReplacer extends Fallback {


	public ApposReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}

	public void doIt() {
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;
				
		String apposEPvalue = "APPOS_REL";
		
		log.info("============== Fallback Generation -- ApposReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				pre.preprocess(oriPair.getGenOriCand());
			} else {
				pre.preprocess(oriPair.getOriSent());
			}
			
			sentence = pre.getSentences()[0];
			MRS mrs = oriPair.getOriMrs();
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(apposEPvalue)) {
					/*
					 * pipe: The man after Hurricane Katrina did not cause a big collapse.
					 * -> The man after which Hurricane did not cause a big collapse.
					 */
					// Hurricane
					String arg1 = ep.getValueByFeature("ARG1");
					// Katrina
					String arg2 = ep.getValueByFeature("ARG2");
					ArrayList<ElementaryPredication> argList;
					argList = mrs.getEPbyFeatAndValue("ARG0", arg1);
					if (argList == null) continue;

					int cfromArg1 = argList.get(0).getCfrom();
					int ctoArg1 = argList.get(0).getCto();
					for (ElementaryPredication e:argList) {
						if (e.getCfrom()<cfromArg1) cfromArg1=e.getCfrom();
						if (e.getCto()>ctoArg1) ctoArg1=e.getCto();
					}
					if (cfromArg1 < 0 || ctoArg1 < 0) continue;
					
					argList = mrs.getEPbyFeatAndValue("ARG0", arg2);
					if (argList == null) continue;

					int cfromArg2 = argList.get(0).getCfrom();
					int ctoArg2 = argList.get(0).getCto();
					for (ElementaryPredication e:argList) {
						if (e.getCfrom()<cfromArg2) cfromArg2=e.getCfrom();
						if (e.getCto()>ctoArg2) ctoArg2=e.getCto();
					}
					if (cfromArg2 < 0 || ctoArg2 < 0) continue;
					
					// Some decomposed (thus shorter) sentence has an MRS from the original one,
					// thus the cto index might exceed the sentence length.
					// It's not easy to get the exact MRS for the decomposed one (especially
					// when parsing gives multiple results), so we just continue;
					if (ctoArg1>sentence.length()||ctoArg2>sentence.length()) continue;
					
					// replace arg2 with arg1
					// The man after Hurricane Hurricane did not cause a big collapse.
					tranSent = sentence.substring(0, cfromArg2);
					tranSent += sentence.substring(cfromArg1, ctoArg1);
					tranSent += sentence.substring(ctoArg2);
					
					// replace arg1 with "which"
					// The man after which Hurricane did not cause a big collapse.
					tranSent = tranSent.substring(0, cfromArg1) + "which" + tranSent.substring(ctoArg1);

					if (tranSent.substring(tranSent.length()-1).equals("."))
						tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
					else tranSent = tranSent + "?";
					
					generate(tranSent, "WHICH", "ApposReplacer");
				}
			}
		}
	}

}
