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
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}

			pre.preprocess(sentence);
			MRS mrs = oriPair.getOriMrs();

			ElementaryPredication oldEP = null;

			ArrayList<ElementaryPredication> eps = mrs.getEps();
			ElementaryPredication ep;

			for (int i=0; i<eps.size(); i++) {
				ep = eps.get(i);
				if (ep.getTypeName().equals(beEPvalue)) {
					if (oldEP != null && oldEP.getTypeName().equals(beEPvalue)) continue;
					int cfrom = ep.getCfrom();
					int cto = ep.getCto();

					if (cfrom > sentence.length() || cto > sentence.length()) continue;

					String carg = ep.getValueByFeature("CARG");

					// TODO: determine more smartly whether the noun is countable or not.
					String qWord = "how many ";
					if (i+1 <= eps.size()) {
						String num = eps.get(i+1).getValueVarByFeature("ARG0").getExtrapair().get("NUM");
						if (num != null && num.equals("SG"))
							qWord = "how much ";
					}
					if (carg!=null && carg.equals((sentence.substring(cfrom, cto)))) {
						tranSent = sentence.substring(0, cfrom) + qWord + sentence.substring(cto);
					} else if (carg!=null && sentence.contains(carg)) {
						tranSent = sentence.replaceAll(carg, qWord);
					} else {
						tranSent = sentence.substring(0, cfrom) + qWord + sentence.substring(cto);
					}

					tranSent = changeQuestionMark(tranSent);
					generate(tranSent, "HOW MANY/MUCH", "NumReplacer");

					oldEP = ep;
				}
			}
		}
	}
}
