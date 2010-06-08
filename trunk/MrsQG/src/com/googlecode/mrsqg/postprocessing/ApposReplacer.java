/**
 *
Is this really apposition? could also be compound (different ARG1/2 ordering)
pipe: The accident after Hurricane Katrina did not cause a big collapse.
-> The accident after which Hurricane did not cause a big collapse.

The girl Anna likes the dog Bart.

bug:

The girl Anna likes which the dog ?
Which the girl likes the dog Bart?
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		int aLargeNum = 1000000;

		String apposEPvalue = "APPOS_REL";

		log.info("============== Fallback Generation -- ApposReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}

			pre.preprocess(sentence);

			MRS mrs = oriPair.getOriMrs();
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(apposEPvalue)) {
					/*
					 * pipe: The man after Hurricane Katrina did not cause a big collapse.
					 * -> The man after which Hurricane did not cause a big collapse.
					 */
					// Hurricane, the girl
					String arg1 = ep.getValueByFeature("ARG1");
					// Katrina, Anna
					String arg2 = ep.getValueByFeature("ARG2");

					ElementaryPredication arg1EP = mrs.getCharVariableMap().get(arg1);
					ElementaryPredication arg2EP = mrs.getCharVariableMap().get(arg2);

					if (arg1EP==null || arg2EP==null) continue;

					HashSet<ElementaryPredication>  arg1Set = arg1EP.getGovernorsByNonArg();
					HashSet<ElementaryPredication>  arg2Set = arg2EP.getGovernorsByNonArg();
					arg2Set.add(arg2EP);

					// we need to find the range of arg2Set to delete it.
					int deleteArg2From = aLargeNum, deleteArg2To = -1;
					for (ElementaryPredication e:arg2Set) {
						if (e.getCfrom()<deleteArg2From) deleteArg2From = e.getCfrom();
						if (e.getCto()>deleteArg2To) deleteArg2To = e.getCto();
					}

					/*
					 * for arg1 ("the girl") we need to replace the quantifier with "which",
					 * or ("girls") insert "which" before it if no quantifier is present.
					 */
					int deleteArg1From = aLargeNum, deleteArg1To = -1;
					for (ElementaryPredication e:arg1Set) {
						if (e.getTypeName().contains("_Q_") && e.getCto() < arg1EP.getCfrom()) {
							deleteArg1From = e.getCfrom();
							deleteArg1To = e.getCto();
							break;
						}
					}
					int insertArg1From = arg1EP.getCfrom();
					if (deleteArg2From < deleteArg1To) {
						log.error("deleteArg2From "+deleteArg2From+" is smaller than deleteArg1To "+deleteArg1To);
						log.error("arg1Set:\n"+arg1Set);
						log.error("arg2Set:\n"+arg2Set);
						log.error("Debug your code!");
					}

					// replace arg2 with arg1
					// The man after Hurricane Hurricane did not cause a big collapse.
					if (deleteArg1From!=aLargeNum) {
						tranSent = sentence.substring(0, deleteArg1From) + " which " +
							sentence.substring(deleteArg1To, deleteArg2From) +
							sentence.substring(deleteArg2To);
					} else {
						tranSent = sentence.substring(0, insertArg1From) + " which " +
						sentence.substring(insertArg1From, deleteArg2From) +
						sentence.substring(deleteArg2To);
					}

					Pattern punct = Pattern.compile(".*\\p{Punct}$");
					Matcher m = punct.matcher(tranSent);
					// it ends with a punctuation but not a ?
					if (m.matches() && !tranSent.substring(tranSent.length()-1).equals("?"))
						tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
					else tranSent = tranSent + "?";

					generate(tranSent, "WHICH", "ApposReplacer");
				}
			}
		}
	}

}
