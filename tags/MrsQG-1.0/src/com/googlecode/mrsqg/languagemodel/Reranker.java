package com.googlecode.mrsqg.languagemodel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.util.StringUtils;

import kylm.model.ngram.NgramLM;
import kylm.model.ngram.reader.ArpaNgramReader;
import kylm.model.ngram.reader.NgramReader;
import kylm.model.ngram.reader.SerializedNgramReader;
import kylm.reader.TextStreamSentenceReader;
import kylm.util.KylmTextUtils;

/**
 * A ranker class to rank questions. It uses the Kyoto Language Modeling Toolkit
 * <a href="http://www.phontron.com/kylm/">kylm</a> to read language models.
 * Detailed explanation is in Section 4.3 of the author's master thesis.
 *
 * @author Xuchen Yao
 *
 */
public class Reranker {

	private NgramLM lm;

	private static Logger log = Logger.getLogger(Reranker.class);

	/**
	 * Read in a language model file.
	 *
	 * It can read "binary" file which is serialized object file from kylm
	 * @param lmFile a language model file
	 * @param binary whether the file is binary or not.
	 */
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
		float prob = Float.NEGATIVE_INFINITY;
		try {
		 prob = lm.getSentenceProbNormalized(tokens);
		} catch (Exception e) {
			log.error("Error for sentence "+StringUtils.concatWithSpaces(tokens), e);
		}
		return prob;
	}

	/**
	 * Given a file containing one sentence per line, output the
	 * probability of each sentence.
	 * @param file the file location
	 */
	public void batchJob(String file) {

		// get the input stream to load the input
		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			log.error("Error:", e);
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
	 * Normalize log-likelihood scores to fit the range [0,1].
	 * The equation is based on that sum(normalized_scores) = 1
	 */
	public static double[] normalize(double[] scores) {
		if (scores==null) return null;
		int size = scores.length;
		double[] normalized = new double[size];
		double z = 0.0, p;

		for (int i=0; i<size; i++) {
			p = Math.pow(10.0, scores[i]);
			z += p;
			normalized[i] = p;
		}

		for (int i=0; i<size; i++) {
			normalized[i] /= z;
		}

		return normalized;
	}

	/**
	 * Calculate the Fbeta value for each element pair in <code>maxEntScores</code> and <code>lmScores</code>.
	 * Reference: <a href="http://en.wikipedia.org/wiki/F1_score">F1 score</a>
	 * @param maxEntScores an array of doubles
	 * @param lmScores an array of doubles
	 * @param beta weight, 1 means unbiased, 0.5 weighs maxEntScores twice as much as lmScores
	 * @return weighted scores, or the other array if one array is null
	 */
	public static double[] Fbeta(double[] maxEntScores, double[] lmScores, double beta) {
		if (maxEntScores==null && lmScores==null) return null;
		else if (maxEntScores==null) return lmScores;
		else if (lmScores==null) return maxEntScores;
		int size = lmScores.length;
		if (size != maxEntScores.length) {
			log.error("maxEntScores and lmScores must have equal length!");
			return null;
		}
		double[] weighted = new double[size];
		for (int i=0; i<size; i++) {
			weighted[i] = (1.0+beta*beta) * maxEntScores[i] * lmScores[i]
			             / (beta*beta*maxEntScores[i] + lmScores[i]);
		}

		return weighted;
	}

	/**
	 * Given a language model in args[0] and a file in args[1] containing
	 * one sentence per line, output the probability of each sentence.
	 * @param args
	 */
	public static void main(String[] args) {
//		String lmFile = "/home/xcyao/lm/corpus/questions/questions.en.rclean.adapt.plm";
//		String batchFile = "/home/xcyao/lm/corpus/questions/batch.txt";
//
//		Reranker r = new Reranker(lmFile, false);
//		r.batchJob(batchFile);
		Reranker r = new Reranker(args[0], false);
		r.batchJob(args[1]);

	}

}
