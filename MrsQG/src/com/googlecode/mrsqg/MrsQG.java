package com.googlecode.mrsqg;

import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.indices.FunctionWords;
import com.googlecode.mrsqg.nlp.indices.IrregularVerbs;
import com.googlecode.mrsqg.nlp.indices.Prepositions;
import com.googlecode.mrsqg.nlp.indices.WordFrequencies;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;
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
	 * LKB used for generation from MRS in xml
	 */
	private LKB lkb = null;
	
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
				if (lkb != null) lkb.exit();
				log.info("MrsQG ended at "+getTimestamp());
				System.exit(0);
			}
			
			if (input.startsWith("mrx: ")||input.startsWith("MRX: ")) {
				String fileLine = input.substring(4).trim();
				File file = new File(fileLine);
				MrsTransformer t = new MrsTransformer(file, p);
				t.transform(true);
			} else if (input.startsWith("pipe: ")) {
				// do everything in an automatic pipeline
				input = input.substring(5).trim();
				
				// pre-processing, get the output FSC XML in a string fsc
				p = new Preprocessor();
				String fsc = p.getFSCbyTerms(input);
				log.info("\nFSC XML from preprocessing:\n");
				log.info(fsc);
				
				// parsing fsc with cheap
				parser.parse(fsc);
				// the number of MRS in the list depends on 
				// the option "-results=" in cheap.
				// Usually it's 3.
				ArrayList<MRS> mrxList = parser.getParsedMRSlist();
				
				// TODO: add MRS selection here
				
				String mrx;
				MrsTransformer t;
				if (mrxList != null) {
					int i=0;
					for (MRS m:mrxList) {
						int countType = 0;
						int countNum = 0;
						i++;
						m.changeFromUnkToNamed();
						mrx = m.toMRXstring();
						
						// generate from original sentence
						lkb.sendMrxToGen(mrx);
						log.info("\nGenerate from original sentence:\n");
						log.info(lkb.getGenSentences());
						log.info("\nFrom the following MRS:\n");
						log.info(mrx);
						log.info(m);
						
						// transform
						t = new MrsTransformer(mrx, p);
						ArrayList<MRS> trMrsList = t.transform(false);
						
						// generate question
						for (MRS qmrs:trMrsList) {
							mrx = qmrs.toMRXstring();
							
							// generate from original sentence
							lkb.sendMrxToGen(mrx);
							log.info("\nGenerated Questions:");
							ArrayList<String> genSentList = lkb.getGenSentences();
							if (genSentList != null) {
								countType++;
								countNum += genSentList.size();
								log.info(genSentList);
							} else {
								// generation failure
								genSentList = lkb.getFailedGenSentences();
								if (genSentList != null) {
									log.warn("Generation failure. *gen-chart* summary:");
									log.warn(genSentList);
								}
							}
							log.info("\nFrom the following MRS:\n");
							log.info(mrx);
							log.info(qmrs);
						}
						log.info(String.format("Cheap MRS %d generates " +
								"%d questions of %d types.", i, countNum, countType));
					}
				}
				
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
		
		// init the LKB generator
		if (prop.getProperty("runLkbPipeline").equalsIgnoreCase("yes")) {
			System.out.println("Creating LKB...");
			// Set Cheap to take FSC as input 
			lkb = new LKB(false);
			
			if (! lkb.isSuccess()) {
				log.error("LKB is not started properly.");
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
		System.out.println("Now turn off your email client, instant messenger, put a " +
				"\"Do Not Disturb\" sign outside your door,\n\tsend your secretary home " +
				", order a takeout and start working.;-)");
		printUsage();
	}
	
	public static void printUsage() {
		System.out.println("\nUsage:");
		System.out.println("\t1. Input the following line:");
		System.out.println("\t\tpipe: a declrative sentence ending with a full stop.");
		System.out.println("\t\tMrsQG generates a question through pipelines of PET and LKB.");
		System.out.println("\t2. Input a declrative sentence at prompt, MrsQG generates the pre-processed FSC in XML.");
		System.out.println("\t\tThen you can copy/paste this FSC into cheap to parse.");
		System.out.println("\t3. Input the following line:");
		System.out.println("\t\tmrx: an declrative MRS XML (MRX) file.");
		System.out.println("\t\tMrsQG reads this MRX and transforms it into interrogative MRX.");
		System.out.println("\t\tThen you can copy/paste the transformed MRX to LKB for generation.");
		System.out.println();
	}
}
