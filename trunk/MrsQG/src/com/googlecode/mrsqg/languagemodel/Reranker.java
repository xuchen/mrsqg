/**
 *
 */
package com.googlecode.mrsqg.languagemodel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.googlecode.mrsqg.util.StringUtils;

import kylm.model.ngram.NgramLM;
import kylm.model.ngram.reader.ArpaNgramReader;
import kylm.model.ngram.reader.NgramReader;
import kylm.model.ngram.reader.SerializedNgramReader;
import kylm.reader.TextStreamSentenceReader;
import kylm.util.KylmTextUtils;

/**
 * @author Xuchen Yao
 *
 */
public class Reranker {

	private NgramLM lm;

	public Reranker(String lmFile, boolean binary) {
		NgramReader nr;
		if (binary)
			nr = new SerializedNgramReader();
		else
			nr = new ArpaNgramReader();
		try { lm = nr.read(lmFile); } catch(IOException e) {
			System.err.println("Problem reading model from file "+lmFile+": "+e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Calculate the normalized log likelihood of a sentence
	 * @param tokens a tokenized string list representing a sentence
	 * @return log10 of normalized sentence probability (<=0)
	 */
	public float rank(String[] tokens) {
		tokens = StringUtils.addStartEnd(tokens);
		float prob = lm.getSentenceProbNormalized(tokens);
		return prob;
	}

	public void batchJob(String file) {

		// get the input stream to load the input
		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		TextStreamSentenceReader tssl = new TextStreamSentenceReader(is);

		for(String[] sent : tssl) {
			float prob = lm.getSentenceProb(sent);
			System.out.println("Log likelihood of sentence \""+KylmTextUtils.concatWithSpaces(sent)+
					"\": "+prob+"("+prob/-sent.length+")");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String lmFile = "/home/xcyao/lm/corpus/questions/questions.en.rclean.adapt.plm";
		String batchFile = "/home/xcyao/lm/corpus/questions/batch.txt";

		Reranker r = new Reranker(lmFile, false);
		r.batchJob(batchFile);

	}

}
