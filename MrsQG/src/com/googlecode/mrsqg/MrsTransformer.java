package com.googlecode.mrsqg;



import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.analysis.TermExtractor;
import com.googlecode.mrsqg.nlp.NETagger;
import com.googlecode.mrsqg.nlp.OpenNLP;
import com.googlecode.mrsqg.util.Dictionary;
import com.googlecode.mrsqg.util.StringUtils;



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
	//private boolean[] firstCapitalize;
	/** <code>Dictionaries</code> for term extraction. */
	private static ArrayList<Dictionary> dicts = new ArrayList<Dictionary>();
	
	public boolean preprocess (String sents) {
		log.debug("Preprocessing");
		
		String[] originalSentences = OpenNLP.sentDetect(sents);
		this.countOfSents = originalSentences.length;
		log.debug("Count of original one: "+countOfSents);
		
		String original;
		this.tokens = new String[countOfSents][];
		this.sentences = new String[countOfSents];
		for (int i = 0; i < countOfSents; i++) {
			original = originalSentences[i];
			log.debug("Sentence "+i+" :"+original);
			tokens[i] = NETagger.tokenize(original);
			sentences[i] = StringUtils.concatWithSpaces(this.tokens[i]);
		}
		
		this.terms = new Term[this.countOfSents][];
		// extract named entities
		this.nes = NETagger.extractNes(this.tokens);
		if (this.nes != null) {
			for (int i=0; i<this.countOfSents; i++){
				original = originalSentences[i];
				this.terms[i] = TermExtractor.getTerms(original, "", this.nes[i],
						MrsTransformer.getDictionaries());
			}
		}
		
		return true;
	}
	

	public static void addDictionary(Dictionary dict) {
		dicts.add(dict);
	}
	
	/**
	 * Returns the <code>Dictionaries</code>.
	 * 
	 * @return dictionaries
	 */
	public static Dictionary[] getDictionaries() {
		return dicts.toArray(new Dictionary[dicts.size()]);
	}	
	/**
	 * Unregisters all <code>Dictionaries</code>.
	 */
	public static void clearDictionaries() {
		dicts.clear();
	}
	
	public static void main(String[] args) {
		String answers  = "Al Gore was born in Washington DC.";
		MrsTransformer t = new MrsTransformer();
		// possibly fail because of dict is not loaded
		t.preprocess(answers);
	}

}
