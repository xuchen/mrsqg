package de.uni_saarland.coli.MrsQG;

import de.uni_saarland.coli.MrsQG.nlp.SnowballStemmer;
import de.uni_saarland.coli.MrsQG.nlp.NETagger;
import de.uni_saarland.coli.MrsQG.nlp.StanfordNeTagger;
import de.uni_saarland.coli.MrsQG.nlp.OpenNLP;
import de.uni_saarland.coli.MrsQG.nlp.LingPipe;

import java.text.SimpleDateFormat;
import org.apache.log4j.PropertyConfigurator;


public class MrsQG {
	/** Apache logger */
	private static org.apache.log4j.Logger log;
	
	/** The directory of MrsQG, required when MrsQG is used as an API. */
	protected String dir;
	
	/**	the DateFormat object used in getTimespamt
	 */
	private static SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	/**
	 * @return	a timestamp String for logging
	 */
	public static synchronized String getTimestamp() {
		return timestampFormatter.format(System.currentTimeMillis());
	}
	
	public static void main(String[] args) {
		
		// initialize Ephyra and start command line interface
		(new MrsQG()).commandLine();
	}
	
	
	/**
	 * Reads a line from the command prompt.
	 * 
	 * @return user input
	 */
	protected String readLine() {
		try {
			return new java.io.BufferedReader(new
				java.io.InputStreamReader(System.in)).readLine();
		}
		catch(java.io.IOException e) {
			return new String("");
		}
	}
	
	/**
	 * <p>A command line interface for MrsQG.</p>
	 * 
	 * <p>The command <code>exit</code> can be used to quit the program.</p>
	 */
	public void commandLine() {
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				log.info("MrsQG ended at "+getTimestamp());
				System.exit(0);
			}
		}
	}
	
	
	/**
	 * <p>Creates a new instance of MrsQG and initializes the system.</p>
	 * 
	 * <p>For use as a standalone system.</p>
	 */
	protected MrsQG() {
		this("");
	}
	
	/**
	 * <p>Creates a new instance of MrsQG and initializes the system.</p>
	 * 
	 * <p>For use as an API.</p>
	 * 
	 * @param dir directory of MrsQG
	 */
	public MrsQG(String dir) {
		this.dir = dir;
		
		// get logging working
		PropertyConfigurator.configure("conf/log4j.properties");
		log = org.apache.log4j.Logger.getLogger(MrsQG.class);
		log.info("MrsQG started at "+getTimestamp());
		
		// create tokenizer
		System.out.println("Creating tokenizer...");
		if (!OpenNLP.createTokenizer(dir +
				"res/nlp/tokenizer/opennlp/EnglishTok.bin.gz"))
			System.err.println("Could not create tokenizer.");
		LingPipe.createTokenizer();
		
		// create sentence detector
		System.out.println("Creating sentence detector...");
		if (!OpenNLP.createSentenceDetector(dir +
				"res/nlp/sentencedetector/opennlp/EnglishSD.bin.gz"))
			System.err.println("Could not create sentence detector.");
		LingPipe.createSentenceDetector();
		
		// create stemmer
		System.out.println("Creating stemmer...");
		SnowballStemmer.create();
		
		// create part of speech tagger
		System.out.println("Creating POS tagger...");
		if (!OpenNLP.createPosTagger(
				dir + "res/nlp/postagger/opennlp/tag.bin.gz",
				dir + "res/nlp/postagger/opennlp/tagdict"))
			System.err.println("Could not create OpenNLP POS tagger.");
		
		// create named entity taggers
		System.out.println("Creating NE taggers...");
		NETagger.loadListTaggers(dir + "res/nlp/netagger/lists/");
		NETagger.loadRegExTaggers(dir + "res/nlp/netagger/patterns.lst");
		System.out.println("  ...loading Standford NETagger");
//		if (!NETagger.loadNameFinders(dir + "res/nlp/netagger/opennlp/"))
//			System.err.println("Could not create OpenNLP NE tagger.");
		if (!StanfordNeTagger.isInitialized() && !StanfordNeTagger.init())
			System.err.println("Could not create Stanford NE tagger.");
		
		System.out.println("  ...done");
	}
	
}
