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
import com.googlecode.mrsqg.util.StringUtils;

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
		final String B_NP = "B-NP";
		final String I_NP = "I-NP";
		final String NP = "NP";
		String sentence;
		String tranSent = null;
		String chunkTag;
		String chunks[];
		String tokens[];
		
		log.info("============== MrsReplacer Generation -- ChunkReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			chunks = pre.getChunks()[0];
			tokens = pre.getTokens()[0];
			if (chunks==null || chunks.length==0) continue;
			
			boolean inNP = false;
			int startNP=-1, endNP;
			tranSent = null;
			for (int i=0; i<chunks.length; i++) {
				chunkTag = chunks[i];
				
				if (chunkTag.equals(B_NP)) {
					startNP = i;
					inNP = true;
				} else if ((inNP && !(chunkTag.equals(B_NP)||chunkTag.equals(I_NP)))
						|| (i==chunks.length-1 && chunkTag.contains(NP))) {
					endNP = i;
					inNP = false;
					if (startNP==0)
						tranSent = "What "+StringUtils.concatWithSpaces(tokens, endNP, tokens.length);
					else
						tranSent = StringUtils.concatWithSpaces(tokens, 0, startNP) + " what " +
							StringUtils.concatWithSpaces(tokens, endNP, tokens.length);
					tranSent = changeQuestionMark(tranSent);		
					log.info("DEBUG: "+tranSent);
					generate(tranSent, "WHAT", "ChunkReplacer");
				}

			}
		}
	}

}
