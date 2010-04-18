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
public class ChunkReplacer extends Fallback {

	public ChunkReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}
	
	public void doIt() {
		
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		String sentence;
		String tranSent;
		String chunks[];
		
		log.info("============== MrsReplacer Generation -- ChunkReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			chunks = pre.getChunks()[0];
			if (chunks==null || chunks.length==0) continue;
			
			for (String chunk:chunks) {
				if (sentence.contains(chunk)) {
					tranSent = sentence.replace(chunk, "what");

					tranSent = changeQuestionMark(tranSent);					
					generate(tranSent, "WHAT", "ChunkReplacer");
				}
			}
		}
	}

}
