/**
 *
pipe: Jackson was killed on August 29, 1958 in Gary, Indiana.
pipe: Jackson was killed in Gary, Indiana on August 29, 1958.
pipe: much of the value of a business is concentrated in the value of its information .
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
				pre.preprocess(oriPair.getGenOriCand());
			} else {
				pre.preprocess(oriPair.getOriSent());
			}
			
			sentence = pre.getSentences()[0];
			MRS mrs = oriPair.getOriMrs();
			
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(inEPvalue)) {
					int cfrom = ep.getCfrom();

					
					// find out this in-phrase and mark every EP with a true flag
					boolean beforeIn = true;
					HashSet<String> xSet = new HashSet<String>();
					// initial set is all the ARG* values of _IN_P_REL
					for (String arg:ep.getAllARGvalue()) {
						if (arg.startsWith("x"))
							xSet.add(arg);
					}
					
					for (ElementaryPredication e:mrs.getEps()) {
						if (e==ep) beforeIn = false;
						if (beforeIn) continue;
						for (String arg:e.getAllARGvalue()) {
							if (arg.startsWith("x") && xSet.contains(arg)) {
								e.setFlag(true);
								xSet.addAll(e.getAllARGvalue());
								break;
							}
						}
					}
					
					// find out the right-most index of this in-phrase
					int cto = ep.getCto();
					for (ElementaryPredication e:mrs.getEps()) {
						if (e.getFlag() && e.getCto() > cto)
							cto = e.getCto();							
					}
					
					mrs.setAllFlag(false);

					
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
