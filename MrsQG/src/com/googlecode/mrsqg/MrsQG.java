package com.googlecode.mrsqg;

import com.googlecode.mrsqg.nlp.indices.FunctionWords;
import com.googlecode.mrsqg.nlp.indices.IrregularVerbs;
import com.googlecode.mrsqg.nlp.indices.Prepositions;
import com.googlecode.mrsqg.nlp.indices.WordFrequencies;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LingPipe;
import com.googlecode.mrsqg.nlp.NETagger;
import com.googlecode.mrsqg.nlp.OpenNLP;
import com.googlecode.mrsqg.nlp.SnowballStemmer;
import com.googlecode.mrsqg.nlp.StanfordNeTagger;
import com.googlecode.mrsqg.nlp.semantics.ontologies.Ontology;
import com.googlecode.mrsqg.nlp.semantics.ontologies.WordNet;


public class MrsQG {
	/** Apache logger */
	private static org.apache.log4j.Logger log;
	
	/** The directory of MrsQG, required when MrsQG is used as an API. */
	protected String dir;
	
	/**	the DateFormat object used in getTimespamt
	 */
	private static SimpleDateFormat timestampFormatter; 
	
	/**
	 * configurations for MrsQG
	 */
	public final String propertyFile = "conf/mrsqg.properties";
	
	/**
	 * a parser used for producing MRS in xml
	 */
	private Cheap parser = null;
	
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
		Preprocessor p = null;
		
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				if (parser!=null) parser.exit();
				log.info("MrsQG ended at "+getTimestamp());
				System.exit(0);
			}
			
			if (input.startsWith("mrx: ")||input.startsWith("MRX: ")) {
				String fileLine = input.substring(5).trim();
				MrsTransformer t = new MrsTransformer(fileLine, p);
				t.transform();
			} else if (input.startsWith("pipe: ")) {
				// do everything in an automatic pipeline
				input = input.substring(6).trim();
				
				// pre-processing, get the output FSC XML in a string fsc
				p = new Preprocessor();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				p.preprocess(input);
				p.outputFSCbyTerms(os);
				String fsc = os.toString();
				
				System.out.println(fsc);
				// parsing fsc with cheap
				parser.parse(fsc);
				System.out.println(parser.getParsedMRSlist());
				
			} else {
				p = new Preprocessor();
				p.preprocess(input);
				p.outputFSCbyTerms(System.out);
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
		
		timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.dir = dir;
		
		// get logging working
		PropertyConfigurator.configure("conf/log4j.properties");
		log = org.apache.log4j.Logger.getLogger(MrsQG.class);
		log.info("MrsQG started at "+getTimestamp());
		
		// read configuration
		Properties prop = new Properties();
		try { 
			prop.load(new FileInputStream(propertyFile)); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// init the cheap parser
		if (prop.getProperty("runCheapPipeline").equalsIgnoreCase("yes")) {
			System.out.println("Creating parser...");
			// Set Cheap to take FSC as input 
			parser = new Cheap(true);
			
			if (! parser.isSuccess()) {
				log.error("cheap is not started properly.");
			}
		}
		
		// create WordNet dictionary
		System.out.println("Creating WordNet dictionary...");
		if (!WordNet.initialize(dir +
				"res/ontologies/wordnet/file_properties.xml"))
			System.err.println("Could not create WordNet dictionary.");
		
		// init wordnet
		Ontology wordNet = new WordNet();
		// - dictionaries for term extraction
		Preprocessor.clearDictionaries();
		Preprocessor.addDictionary(wordNet);
		

		// load function words (numbers are excluded)
		System.out.println("Loading function verbs...");
		if (!FunctionWords.loadIndex(dir +
				"res/indices/functionwords_nonumbers"))
			System.err.println("Could not load function words.");
		
		// load prepositions
		System.out.println("Loading prepositions...");
		if (!Prepositions.loadIndex(dir +
				"res/indices/prepositions"))
			System.err.println("Could not load prepositions.");
		
		// load irregular verbs
		System.out.println("Loading irregular verbs...");
		if (!IrregularVerbs.loadVerbs(dir + "res/indices/irregularverbs"))
			System.err.println("Could not load irregular verbs.");
		
		// load word frequencies
		System.out.println("Loading word frequencies...");
		if (!WordFrequencies.loadIndex(dir + "res/indices/wordfrequencies"))
			System.err.println("Could not load word frequencies.");

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
		
		// create chunker
		System.out.println("Creating chunker...");
		if (!OpenNLP.createChunker(dir +
				"res/nlp/phrasechunker/opennlp/EnglishChunk.bin.gz"))
			System.err.println("Could not create chunker.");
		
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
