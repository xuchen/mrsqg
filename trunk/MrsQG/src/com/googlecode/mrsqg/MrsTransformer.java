package com.googlecode.mrsqg;


import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.nlp.OpenNLP;



/**
 * This class transforms the MRS of a declarative sentence 
 * into that of a interrogative sentence. 
 * 
 * @author Xuchen Yao
 * @version 2010-02-26
 */
public class MrsTransformer {
	
	private static Logger log = Logger.getLogger(MrsTransformer.class);
	
	private int countOfSents;
	private String[] originalSentences;
	private String[][] tokens;
	private String[] sentences;
	private String[][][] nes;
	//private ArrayList<String>[] to;
	private Term[][] terms;
	private boolean[] firstCapitalize;
	
	public static boolean preprocess (String sents) {
		log.debug("Preprocessing");
		
		String[] originalSentences = OpenNLP.sentDetect(sents);
		int countOfSents = originalSentences.length;
		log.debug("Count of original one: "+countOfSents);
		
		return true;
	}

}
