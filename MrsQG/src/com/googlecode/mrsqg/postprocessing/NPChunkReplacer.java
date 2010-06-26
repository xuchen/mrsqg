/**
 *
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;
import com.googlecode.mrsqg.util.StringUtils;

/**
 *
 * @author Xuchen Yao
 *
 */
public class NPChunkReplacer extends Fallback {

	public NPChunkReplacer(Cheap cheap, LKB lkb, ArrayList<Pair> oriPairs) {
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

		log.info("============== MrsReplacer Generation -- NPChunkReplacer==============");

		for (Pair oriPair:oriPairs) {
			if (oriPair.getGenOriCand()!=null) {
				sentence = oriPair.getGenOriCand();
			} else {
				sentence = oriPair.getOriSent();
			}

			pre.preprocess(sentence);
			chunks = pre.getNpChunks()[0];
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
				} else if (chunkTag.equals(I_NP)) {
					inNP = true;
				} else if ((inNP && !(chunkTag.equals(B_NP)||chunkTag.equals(I_NP)))
						|| (i==chunks.length-1 && chunkTag.contains(NP))) {
					endNP = i;
					inNP = false;

					String qWord = " what ";
					for (Term term:pre.getTerms()[0]) {
						if (term.getFrom()>= startNP && term.getTo() <= endNP) {
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
					if (startNP==0)
						tranSent = qWord+StringUtils.concatWithSpaces(tokens, endNP, tokens.length);
					else
						tranSent = StringUtils.concatWithSpaces(tokens, 0, startNP) + qWord +
							StringUtils.concatWithSpaces(tokens, endNP, tokens.length);
					tranSent = changeQuestionMark(tranSent);
					log.info("tranSent: "+tranSent);
					generate(tranSent, "WHAT", "NPChunkReplacer");
				}

			}
		}
	}

}
