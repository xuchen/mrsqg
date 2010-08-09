/**
 *
pipe: Jackson was killed on August 29, 1958 in Gary, Indiana.
pipe: Jackson was killed in Gary, Indiana on August 29, 1958.
pipe: much of the value of a business is concentrated in the value of its information .
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
public class WhereReplacer extends Fallback {


	public WhereReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}

	public void doIt() {

		if (this.oriPairs == null) return;

		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;

		String inEPvalue = "_IN_P_REL";

		log.info("============== MrsReplacer Generation -- WhereReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}

			pre.preprocess(sentence);
			MRS mrs = oriPair.getOriMrs();

			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(inEPvalue)) {

					String arg1 = ep.getValueByFeature("ARG1");
					String arg2 = ep.getValueByFeature("ARG2");
					if (! (arg1.startsWith("e") && arg2.startsWith("x")) || arg1==null || arg2==null)
						/*
						 *  "in" must be attached to a verb (by arg1) and modifies an entity (arg2)
						 *  otherwise we can't ask a "where" question, such as:
						 *  This is the pen in the box => ? This is the pen where?
						 */
						continue;

					int cfrom = ep.getCfrom();

					// every EP related to "in" EP is set to false
					mrs.keepDependentEPbyLabel(arg2, ep);

					// find out the right-most index of this in-phrase
					int cto = ep.getCto();
					for (ElementaryPredication e:mrs.getEps()) {
						if (e.getFlag() == false && e.getCto() > cto)
							cto = e.getCto();
					}

					mrs.setAllFlag(false);

					if (cfrom<0 || cto<0 || cto > sentence.length())
						continue;

					tranSent = sentence.substring(0, cfrom);
					tranSent += "where";
					tranSent += sentence.substring(cto, sentence.length());
					if (tranSent.substring(tranSent.length()-1).equals("."))
						tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
					else tranSent = tranSent + "?";

					generate(tranSent, "WHERE", "WhereReplacer");

				}
			}
		}
	}

}
