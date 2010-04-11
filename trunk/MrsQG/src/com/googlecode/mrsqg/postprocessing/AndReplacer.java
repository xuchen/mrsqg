/**
 * Current this class replaces a coordination phrase (indicated by the
 * _AND_C_REL relation) with "what", and tries to generate from it.
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
public class AndReplacer extends Fallback {


	public AndReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}

	public void doIt () {
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;
		
		String andEPvalue = "_AND_C_REL";
		
		log.info("============== Fallback Generation -- AndReplacer==============");
		
		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()==null) continue;
			pre.preprocess(oriPair.getGenOriCand());
			
			sentence = pre.getSentences()[0];
			MRS mrs = oriPair.getOriMrs();

			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(andEPvalue) && 
						ep.getValueByFeature("L-INDEX") != null &&
						ep.getValueByFeature("R-INDEX") != null &&
						ep.getValueByFeature("L-HNDL") == null &&
						ep.getValueByFeature("R-HNDL") == null) {
					String arg1 = ep.getValueByFeature("L-INDEX");
					String arg2 = ep.getValueByFeature("R-INDEX");
					ArrayList<ElementaryPredication> argList;
					argList = mrs.getEPbyFeatAndValue("ARG0", arg1);
					if (argList == null) continue;
					int cfrom = argList.get(0).getCfrom();
					// find out the left-most index
					for (ElementaryPredication e:argList) {
						if (e.getCfrom()<cfrom) cfrom = e.getCfrom();
					}
					
					argList = mrs.getEPbyFeatAndValue("ARG0", arg2);
					if (argList == null) continue;
					int cto = ep.getCto();
					// find out the right-most index
					for (ElementaryPredication e:argList) {
						if (e.getCto()>cto) cto = e.getCto();
					}
					
					tranSent = sentence.substring(0, cfrom);
					tranSent += "what";
					tranSent += sentence.substring(cto, sentence.length());
					if (tranSent.substring(tranSent.length()-1).equals("."))
						tranSent = tranSent.substring(0, tranSent.length()-1) + "?";
					else tranSent = tranSent + "?";
					
					generate(tranSent, "WHAT", "AndReplacer");

				}
			}
		}	
	}

}
