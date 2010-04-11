package com.googlecode.mrsqg;

import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.mrs.decomposition.*;
import com.googlecode.mrsqg.mrs.selection.PreSelector;
import com.googlecode.mrsqg.nlp.indices.FunctionWords;
import com.googlecode.mrsqg.nlp.indices.IrregularVerbs;
import com.googlecode.mrsqg.nlp.indices.Prepositions;
import com.googlecode.mrsqg.nlp.indices.WordFrequencies;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.googlecode.mrsqg.nlp.*;
import com.googlecode.mrsqg.nlp.semantics.ontologies.Ontology;
import com.googlecode.mrsqg.nlp.semantics.ontologies.WordNet;
import com.googlecode.mrsqg.postprocessing.*;


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
		
		// initialize MrsQG and start command line interface
		MrsQG HelloLady = new MrsQG();;
		try {
			HelloLady.commandLine();
		} catch (Exception e) {
			e.printStackTrace();
			HelloLady.exitAll();
		}
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
	 * Exit everything and release memory properly.
	 */
	public void exitAll() {
		if (parser!=null) parser.exit();
		if (lkb != null) lkb.exit();
		log.info("MrsQG ended at "+getTimestamp());
		System.exit(0);
	}
	
	/**
	 * <p>A command line interface for MrsQG.</p>
	 * 
	 * <p>The command <code>exit</code> can be used to quit the program.</p>
	 */
	public void commandLine() {
		Preprocessor p = null;
		SubordinateDecomposer subordDecomposer = new SubordinateDecomposer();
		CoordDecomposer coordDecomposer = new CoordDecomposer();
		ApposDecomposer apposDecomposer = new ApposDecomposer();
		SubclauseDecomposer subDecomposer = new SubclauseDecomposer();
		WhyDecomposer whyDecomposer = new WhyDecomposer();
		boolean fallback = true;
		
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				exitAll();
			}
			
			if (input.startsWith("mrx: ")||input.startsWith("MRX: ")) {
				String fileLine = input.substring(4).trim();
				File file = new File(fileLine);
				MrsTransformer t = new MrsTransformer(file, p);
				t.transform(true);
			} else if (input.startsWith("lkb: ")) {
				input = input.substring(4).trim();
				lkb.sendInput(input);
				System.out.println(lkb.getRawOutput());
			} else if (input.startsWith("cheap: ")) {
				input = input.substring(6).trim();
				// pre-processing, get the output FSC XML in a string fsc
				p = new Preprocessor();
				String fsc = p.getFSCbyTerms(input, true);
				log.info("\nFSC XML from preprocessing:\n");
				log.info(fsc);
				
				// parsing fsc with cheap
				if (parser == null) continue;
				parser.parse(fsc);
				log.info(parser.getParsedMRSlist());

				if (p.getNumTokens() > 15) {
					parser.releaseMemory();
				}
				//if (!parser.isSuccess()) continue;
			} else if (input.startsWith("pipe: ")) {
				// do everything in an automatic pipeline
				input = input.substring(5).trim();
				
				// pre-processing, get the output FSC XML in a string fsc
				p = new Preprocessor();
				String fsc = p.getFSCbyTerms(input, true);
				//log.info("\nFSC XML from preprocessing:\n");
				//log.info(fsc);
				
				// parsing fsc with cheap
				if (parser == null) continue;
				parser.parse(fsc);
				// the number of MRS in the list depends on 
				// the option "-results=" in cheap.
				// Usually it's 3.
				ArrayList<MRS> origMrsList = parser.getParsedMRSlist();
				ArrayList<MRS> mrxList = PreSelector.doIt(lkb, origMrsList);
				boolean success = parser.isSuccess();
				if (p.getNumTokens() > 15) {
					parser.releaseMemory();
				}
				if (!success || mrxList == null) continue;

				// pairs for declarative sentences, could be original, or decomposed.
				ArrayList<Pair> declSuccPairs = new ArrayList<Pair>();
				ArrayList<Pair> declFailPairs = new ArrayList<Pair>();
				// pairs for successfully generated questions
				ArrayList<Pair> quesSuccPairs = new ArrayList<Pair>();
				// pairs for not successfully generated questions
				ArrayList<Pair> quesFailPairs = new ArrayList<Pair>();
				
				// decomposition
//				ArrayList<MRS> subordDecomposedMrxList = subordDecomposer.doIt(mrxList);
//				ArrayList<MRS> subDecomposedMrxList = subDecomposer.doIt(mrxList);
//				ArrayList<MRS> coordDecomposedMrxList = coordDecomposer.doIt(mrxList);
//				ArrayList<MRS> apposDecomposedMrxList = apposDecomposer.doIt(mrxList);
//				ArrayList<MRS> whyDecomposedMrxList = whyDecomposer.doIt(mrxList);
//				
//				if (subordDecomposedMrxList!=null) mrxList.addAll(0, subordDecomposedMrxList);
//				if (subDecomposedMrxList!=null) mrxList.addAll(0, subDecomposedMrxList);
//				if (coordDecomposedMrxList!=null) mrxList.addAll(0, coordDecomposedMrxList);
//				if (apposDecomposedMrxList!=null) mrxList.addAll(0, apposDecomposedMrxList);
//				if (whyDecomposedMrxList!=null) mrxList.addAll(0, whyDecomposedMrxList);
				
				mrxList = subordDecomposer.doIt(mrxList);
				mrxList = subDecomposer.doIt(mrxList);
				mrxList = coordDecomposer.doIt(mrxList);
				mrxList = apposDecomposer.doIt(mrxList);
				mrxList = whyDecomposer.doIt(mrxList);
				
				
				// generation
				if (mrxList != null && lkb != null) {
					String mrx;
					MrsTransformer t;
					int i=0;
					for (MRS m:mrxList) {
						int countType = 0;
						int countNum = 0;
						i++;
						m.changeFromUnkToNamed();
						mrx = m.toMRXstring();
						
						// generate from original sentence
						lkb.sendMrxToGen(mrx);
						log.info("\nGenerate from the original sentence:\n");
						ArrayList<String> genOriSentList = lkb.getGenSentences();
						log.info(genOriSentList);
						log.info("\nFrom the following MRS:\n");
						log.info(mrx);
						log.info(m);
						
						ArrayList<String> genOriSentFailedList = null;
						if (genOriSentList == null) {
							genOriSentFailedList = lkb.getFailedGenSentences();
						}
						
						if (!(genOriSentList == null && genOriSentFailedList == null)) {
							Pair pair = new Pair(input, m, genOriSentList, genOriSentFailedList);
							if (genOriSentList!=null) declSuccPairs.add(pair);
							else declFailPairs.add(pair);
							
						} else {
							continue;
						}
						
						// transform
						t = new MrsTransformer(m, p);
						ArrayList<MRS> trMrsList = t.transform(false);
						
						if (trMrsList == null) continue;
						// generate question
						for (MRS qmrs:trMrsList) {
							mrx = qmrs.toMRXstring();
							
							// generate from original sentence
							lkb.sendMrxToGen(mrx);
							log.info("\nGenerated Questions:");
							ArrayList<String> genQuesList = lkb.getGenSentences();
							ArrayList<String> genQuesFailedList = null;
							if (genQuesList != null) {
								countType++;
								countNum += genQuesList.size();
								log.info(genQuesList);
							} else {
								// generation failure
								genQuesFailedList = lkb.getFailedGenSentences();
								if (genQuesFailedList != null) {
									log.warn("Generation failure. *gen-chart* summary:");
									log.warn(genQuesFailedList);
								}
							}
							log.info("\nFrom the following MRS:\n");
							log.info(mrx);
							log.info(qmrs);
							
							// Add to pair list
							if (!(genQuesList==null && genQuesFailedList==null)) {
								Pair pair = new Pair (input, m, genOriSentList, genOriSentFailedList,
										qmrs, genQuesList, genQuesFailedList);
								if (genQuesList!=null)	quesSuccPairs.add(pair);
								else quesFailPairs.add(pair);
							}
						}
						log.info(String.format("Cheap MRS %d generates " +
								"%d questions of %d types.", i, countNum, countType));
					}
				}
				
				if (fallback) {
					// fallback
					if (declSuccPairs.size() != 0) {
						Fallback planB = new Fallback (parser, lkb, declSuccPairs);
						planB.doIt();
						ArrayList<Pair> pairs = planB.getGenSuccPairs();
						if (pairs!=null) quesSuccPairs.addAll(pairs);
						pairs = planB.getGenFailPairs();
						if (pairs!=null) quesFailPairs.addAll(pairs);

						AndReplacer andR = new AndReplacer (parser, lkb, declSuccPairs);
						andR.doIt();
						pairs = andR.getGenSuccPairs();
						if (pairs!=null) quesSuccPairs.addAll(pairs);
						pairs = andR.getGenFailPairs();
						if (pairs!=null) quesFailPairs.addAll(pairs);
						
						WhereReplacer whereR = new WhereReplacer (parser, lkb, declSuccPairs);
						whereR.doIt();
						pairs = whereR.getGenSuccPairs();
						if (pairs!=null) quesSuccPairs.addAll(pairs);
						pairs = whereR.getGenFailPairs();
						if (pairs!=null) quesFailPairs.addAll(pairs);
						
						ApposReplacer apposR = new ApposReplacer (parser, lkb, declSuccPairs);
						apposR.doIt();
						pairs = apposR.getGenSuccPairs();
						if (pairs!=null) quesSuccPairs.addAll(pairs);
						pairs = apposR.getGenFailPairs();
						if (pairs!=null) quesFailPairs.addAll(pairs);
					}
				}
				
				// summary
				log.info("===========Summary of Generated Questions============");
				log.info("oriSent: "+input);
				if (quesSuccPairs.size()!=0) {
					for (Pair pair:quesSuccPairs) {
						log.info("\n");
						if (pair.getGenOriCand()!=null) log.info("oriSent: "+pair.getGenOriCand());
						log.info("SentType: "+pair.getQuesMrs().getSentType());
						log.info("Decomposer: "+pair.getQuesMrs().getDecomposer());
						log.info("Question: "+pair.getGenQuesCand());
						log.info(pair.getGenQuesList());
					}
				} else {
					log.info("No questions generated.");
				}
				
			} else {
				p = new Preprocessor();
				p.preprocess(input);
				p.outputFSCbyTerms(System.out, true);
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
				exitAll();
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
		
//		if (lkb!=null && lkb.getDisplay()) {
//			System.out.println("\tYour lkb.properties file sets LKB to show display. " +
//					"If you don't see the LKB window,\n\tthen the program doesn't start " +
//					"properly (this happens occasionally).\n\tInput exit and run MrsQG again.");
//		}
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
		System.out.println("\t4. Input the following line:");
		System.out.println("\t\tlkb: an LKB command");
		System.out.println("\t\tThen MrsQG serves as a wrapper for LKB. You can talk with LKB interactively through the prompt.");
		System.out.println("\t5. Input the following line:");
		System.out.println("\t\tcheap: a sentence");
		System.out.println("\t\tThen MrsQG serves as a wrapper for Cheap. You can talk with cheap interactively through the prompt.");
		System.out.println();
	}
}
