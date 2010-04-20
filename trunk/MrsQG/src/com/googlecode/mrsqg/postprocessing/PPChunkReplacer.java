/**
 * 
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;
import com.googlecode.mrsqg.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class PPChunkReplacer extends Fallback {

	public PPChunkReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
		super(cheap, lkb, oriPairs);
	}
	
	public void doIt() {
		
		if (this.oriPairs == null) return;
		
		Preprocessor pre = new Preprocessor();
		final String B_PP = "B-PP", END = "O";
		final String NP = "NP";
		String sentence, quesWord, targetWord;
		String tranSent, tranSent1;
		String tranSentWhere, tranSentWhen;
		String chunkTag;
		String chunks[], tokens[], pos[];
		
		log.info("============== MrsReplacer Generation -- PPChunkReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}
			
			pre.preprocess(sentence);
			chunks = pre.getPpChunks()[0];
			tokens = pre.getTokens()[0];
			pos = pre.getPos()[0];
			if (chunks==null || chunks.length==0) continue;
			
			boolean inPP = false;
			int startPP=-1, endPP;
			
			for (int i=0; i<chunks.length; i++) {
				tranSent = null;
				tranSent1 = null;
				chunkTag = chunks[i];
				//[B-PP, B-NP, O, B-NP, I-NP, B-VP, B-PP, B-NP, I-NP, O, B-VP, B-NP, I-NP, I-NP, B-PP, B-NP, O, B-NP, I-NP, I-NP, O]
				if (chunkTag.equals(B_PP)) {
					startPP = i+1;
					inPP = true;
				} else if (inPP && chunkTag.contains(NP)) {
					inPP = true;
				} else if ((inPP && (chunkTag.equals(END) || !chunkTag.contains("NP")))
						|| (i==chunks.length-1 && chunkTag.contains(NP))) {
					endPP = i;
					inPP = false;
					quesWord = "what";
					targetWord = "place ";
					for (int j=startPP; j<endPP; j++) {
						if (pos[j].equals("CD"))
							// a number
							targetWord = "time ";
					}
					if (startPP==1) {
						tranSent = tokens[0]+" "+quesWord+" "+targetWord+StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						if (targetWord.equals("place "))
							tranSent1 = tokens[0]+" which "+targetWord+StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						tranSentWhere = "Where " + StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						tranSentWhen = "When " + StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
					} else {
						tranSent = StringUtils.concatWithSpaces(tokens, 0, startPP) + " " +quesWord+ " " +targetWord+
							StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						if (targetWord.equals("place "))
							tranSent1 = StringUtils.concatWithSpaces(tokens, 0, startPP) + " which " +targetWord+
								StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						tranSentWhere = StringUtils.concatWithSpaces(tokens, 0, startPP-1) + " where " +
							StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
						tranSentWhen = StringUtils.concatWithSpaces(tokens, 0, startPP-1) + " when " +
							StringUtils.concatWithSpaces(tokens, endPP, tokens.length);
					}
					
					tranSent = changeQuestionMark(tranSent);	
					log.info("tranSent: "+tranSent);
					generate(tranSent, quesWord.toUpperCase(), "PPChunkReplacer");
					
					tranSentWhere = changeQuestionMark(tranSentWhere);	
					log.info("tranSent: "+tranSentWhere);
					generate(tranSentWhere, "WHERE", "PPChunkReplacer");
					
					tranSentWhen = changeQuestionMark(tranSentWhen);	
					log.info("tranSent: "+tranSentWhen);
					generate(tranSentWhen, "WHEN", "PPChunkReplacer");
					
					if (tranSent1 != null)
						generate(tranSent, "WHICH", "PPChunkReplacer");
				}

			}
		}
	}

}
