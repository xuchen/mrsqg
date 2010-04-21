/**
 * Append a "why" to the front.
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.mrs.decomposition.WhyDecomposer;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;

/**
 * @author Xuchen Yao
 *
 */
public class WhyAppender extends Fallback {

	public WhyAppender(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}
	
	public void doIt() {
		
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;
		
		
		log.info("============== MrsReplacer Generation -- WhyAppender==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			MRS mrs = oriPair.getOriMrs();
			
			// we can't care which is reash, which is result...
			MRS whyMrs = WhyDecomposer.constructWhyMrs(mrs);
			if (whyMrs==null) continue;
			tranSent = "Why " + sentence;
			
			generate(tranSent, "WHY", "WhyAppender");
			
			String mrx = whyMrs.toMRXstring();
			generator.sendMrxToGen(mrx);
			log.info("\nGenerate from fallback sentence:\n");

			ArrayList<String> genQuesList = generator.getGenSentences();
			ArrayList<String> genQuesFailedList = null;
			log.info(genQuesList);
			
			// Add to pair list
			if (!(genQuesList==null && genQuesFailedList==null)) {
				whyMrs.setDecomposer("WhyAppender");
				Pair pair = new Pair (whyMrs, genQuesList, genQuesFailedList);
				pair.setTranSent(tranSent);
				if (genQuesList!=null)	genSuccPairs.add(pair);
				else genFailPairs.add(pair);
			}

		}
	}

}
