package com.googlecode.mrsqg.nlp.indices;


import java.io.IOException;

import com.googlecode.mrsqg.util.Dictionary;
import com.googlecode.mrsqg.util.HashDictionary;

/**
 * <p>A dictionary of prepositions in English.<p>
 * 
 * <p>Internally, it uses an instance of <code>HashDictionary</code> to store
 * the words.</p>
 * 
 * @author Nico Schlaefer
 * @version 2007-04-14
 */
public class Prepositions {
	/** The dictionary containing the prepositions. */
	private static Dictionary dictionary;
	
	/**
	 * Creates the dictionary from a list of prepositions in a file.
	 * 
	 * @param filename file containing the prepositions
	 * @return true, iff the prepositions were loaded successfully
	 */
	public static boolean loadIndex(String filename) {
		try {
			dictionary = new HashDictionary(filename);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Looks up a word in the dictionary.
	 * 
	 * @param word the word to look up
	 * @return true, iff it is a preposition
	 */
	public static boolean lookup(String word) {
		return (dictionary != null && dictionary.contains(word));
	}
}
