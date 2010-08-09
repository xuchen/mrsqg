
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;

/**
 * WhatReplacer functions as one of the fallbacks of MrsQG. It finds out all the
 * arguments of all verbs in a sentence, replace them with a question word according
 * to the undelrying named entities (otherwise with "what") and try to re-generate.
 * e.g. it gives us an easy way to find north by looking at the stars .
 * --> what gives us an easy way to find north by looking at the stars ?
 * --> it gives what an easy way to find north by looking at the stars ?
 * --> it gives us what to find north by looking at the stars ?
 * --> it gives us an easy way to find what by looking at the stars ?
 * --> it gives us an easy way to find north by looking at what ?
 *
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
						for (String arg:ep.getAllARGvalueExceptARG0()) {
							if (!arg.startsWith("x")) continue;

							// every EP related to arg is set to false
							mrs.keepDependentEPbyLabel(arg, ep);

							int cfrom = -1;
							int cto = -1;

							for (ElementaryPredication e:mrs.getEps()) {
								if (e.getFlag() == false) {
									if (cfrom == -1 && e.getCfrom()>cfrom)
										cfrom = e.getCfrom();
									if (e.getCto() > cto)
										cto = e.getCto();
								}
							}
							mrs.setAllFlag(false);

							if (cfrom > sentence.length() || cto > sentence.length() ||
									cfrom < 0 || cto < 0) continue;

							String qWord = " what ";
							for (Term term:pre.getTerms()[0]) {
								if (term.getCfrom()>= cfrom && term.getCto() <= cto) {
									for (String neType:term.getNeTypes()) {
										if (neType.equals("NEperson")||neType.equals("NEfirstName")) {
											qWord = " who ";
											break;
										} else if (neType.equals("NElocation")) {
											qWord = " where ";
											break;
										} else if (neType.equals("NEdate")||neType.equals("NEtime")) {
											qWord = " when ";
											break;
										} else
											qWord = " what ";
									}
								}
								if (!qWord.equals(" what "))
									break;
							}

							tranSent = sentence.substring(0, cfrom) + qWord + sentence.substring(cto);
							tranSent = changeQuestionMark(tranSent);
							generate(tranSent, "WHAT", "WhatReplacer");
						}
					}
				}
			}
		}

}
