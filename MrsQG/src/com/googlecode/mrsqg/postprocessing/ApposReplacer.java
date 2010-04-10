/**
 * 
pipe: The man after Hurricane Katrina did not cause a big collapse.
-> The man after which Hurricane did not cause a big collapse.
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
public class ApposReplacer extends MrsReplacer {

	/**
	 * @param cheap
	 * @param lkb
	 * @param pre
	 * @param list
	 */
	public ApposReplacer(Cheap cheap, LKB lkb, Preprocessor pre,
			ArrayList<MRS> list) {
		super(cheap, lkb, pre, list);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.postprocessing.MrsReplacer#doIt()
	 */
	@Override
	public void doIt() {
		if (this.origList == null) return;
		
		String sentence = pre.getSentences()[0];
		String tranSent;
		Pair pair;
		
		String apposEPvalue = "APPOS_REL";

		for (MRS mrs:origList) {
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
					if (cfromArg1 < 0 || ctoArg1 < 0) continue;
					
					argList = mrs.getEPbyFeatAndValue("ARG0", arg2);
					if (argList == null) continue;

					int cfromArg2 = argList.get(0).getCfrom();
					int ctoArg2 = argList.get(0).getCto();
					if (cfromArg2 < 0 || ctoArg2 < 0) continue;
					
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
					
					pair = new Pair(sentence, tranSent, "WHAT");
					pairs.add(pair);
				}
			}
		}
		

		log.info("============== MrsReplacer Generation -- ApposReplacer==============");
		
		genFromParse();

	}

}
