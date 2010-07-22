package com.googlecode.mrsqg;

import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.evaluation.Instance;
import com.googlecode.mrsqg.evaluation.QGSTEC2010;
import com.googlecode.mrsqg.languagemodel.Reranker;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.mrs.decomposition.*;
import com.googlecode.mrsqg.mrs.selection.PreSelector;
import com.googlecode.mrsqg.nlp.indices.FunctionWords;
import com.googlecode.mrsqg.nlp.indices.IrregularVerbs;
import com.googlecode.mrsqg.nlp.indices.Prepositions;
import com.googlecode.mrsqg.nlp.indices.WordFrequencies;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.googlecode.mrsqg.nlp.*;
import com.googlecode.mrsqg.nlp.semantics.ontologies.Ontology;
import com.googlecode.mrsqg.nlp.semantics.ontologies.WordNet;
import com.googlecode.mrsqg.postprocessing.*;
import com.googlecode.mrsqg.util.StringUtils;


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
	 * a question re-ranker based on language models
	 */
	private Reranker ranker = null;

	/**
	 * Whether run the QGSTEC2010 test
	 */
	private boolean runTest = false;

	/**
	 * An XML test file input
	 */
	private File testFileInput;

	/**
	 * An XML file with results
	 */
	private File testFileOutput;

	/**
	 * whether to apply fallbacks to generate as many questions as possible
	 */
	private boolean fallback;

	// pairs for declarative sentences, could be original, or decomposed.
	private ArrayList<Pair> declSuccPairs;
	private ArrayList<Pair> declFailPairs;
	// pairs for successfully generated questions
	private ArrayList<Pair> quesSuccPairs;
	// pairs for not successfully generated questions
	private ArrayList<Pair> quesFailPairs;

	private QGSTEC2010 QGSTEC2010processor;

	/**
	 * @return	a timestamp String for logging
	 */
	public static synchronized String getTimestamp() {
		return timestampFormatter.format(System.currentTimeMillis());
	}

	public boolean getRunTest () {return runTest;}

	public static void main(String[] args) {

		// initialize MrsQG and start command line interface
		MrsQG HelloLady = new MrsQG();;
		try {
			if (HelloLady.getRunTest()) {
				HelloLady.runTest();
				HelloLady.exitAll();
			}
			else
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
				log.info(parser.getParsedMrxString());
				ArrayList<MRS> list = parser.getParsedMRSlist();
				if (list==null) continue;
				for (MRS mt:list) {
					log.info(mt.toMRXstring());
					log.info(mt);
				}

				if (p.getNumTokens() > 15) {
					parser.releaseMemory();
				}
				//if (!parser.isSuccess()) continue;
			} else if (input.startsWith("pg: ")) {
				input = input.substring(3).trim();
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
				boolean success = parser.isSuccess();
				if (p.getNumTokens() > 15) {
					parser.releaseMemory();
				}
				if (!success) continue;
				String mrx;
				if (origMrsList==null||origMrsList.size()==0) continue;
				log.info("Number of PET parses: "+origMrsList.size());
				if (lkb==null) continue;
				log.info("Sending PET output to LKB...");
				for (MRS m:origMrsList) {
					// generate from original sentence
					m.changeFromUnkToNamed();
					mrx = m.toMRXstring();
					lkb.sendMrxToGen(mrx);
					log.info(lkb.getGenSentences());
					log.info(lkb.getMaxEntScores());
				}

			} else if (input.startsWith("pipe: ")) {
				// do everything in an automatic pipeline
				input = input.substring(5).trim();

				// generate questions based on text
				runPipe(input);

			} else {
				p = new Preprocessor();
				p.preprocess(input);
				p.outputFSCbyTerms(System.out, true);
			}
		}
	}

	public void runTest() {

		ArrayList<Instance> instanceList = QGSTEC2010processor.getInstanceList();
		String text, questionType, question;
		ArrayList <String> quesList;
		boolean success;
		try {
			// append
			FileOutputStream fop=new FileOutputStream(testFileOutput, true);
			for (Instance ins:instanceList) {
				text = ins.getText();

				// generate questions based on text
				success = runPipe(text);

				if (!success) continue;
				log.info("runPipe is done");

				// assign generated question back
				for (int i=0; i<ins.getQuestionTypeList().size(); i++) {
					if (i%2==0) continue;
					questionType = ins.getQuestionTypeList().get(i);
					// retrieve question according to questionType
					quesList = retrieveQuestion(questionType, text);
					if (quesList!=null&&quesList.size()!=0) {
						ins.addGenQuestion(quesList.get(0));
						ins.addGenQuestion(quesList.get(1));
						ins.addToCandidatesList(quesList.toString());
					} else {
						ins.addGenQuestion("");
						ins.addGenQuestion("");
						ins.addToCandidatesList("");
					}
				}
				// append incrementally
				ins.toXML(fop);
				fop.flush();
			}

			// write it overall again
			QGSTEC2010processor.toXML(fop);
			fop.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> retrieveQuestion (String type, String original) {
		String question="";
		if (type == null) return null;
		if (type.equals("yes/no")) type="y/n";
		type = type.toUpperCase();
		ArrayList<String> succList = new ArrayList<String>();
		ArrayList<String> twoList = new ArrayList<String>();
		ArrayList<Pair> pairs = new ArrayList<Pair>();
		if (quesSuccPairs.size() != 0) {
			for (Pair p:quesSuccPairs) {
				if (!p.getFlag() && p.getQuesMrs().getSentType().startsWith(type)) {
					question = p.getGenQuesCand();
					if (!succList.contains(question)) {
						succList.add(question);
						pairs.add(p);
					}
				}
			}
		}

		// try to pull out a sentence from fallbacks that can generate
		if (succList.size() == 0 && quesFailPairs.size() != 0) {
			for (Pair p:quesFailPairs) {
				if (!p.getFlag() && p.getQuesMrs().getSentType().startsWith(type)) {
					question = p.getTranSent();
					if (!succList.contains(question)) {
						succList.add(question);
						pairs.add(p);
					}
				}
			}
		}

		// try to pull out sentences from fallbacks that can't parse
		if (succList.size() == 0) {
			if (quesFailPairs.size() != 0) {
				for (Pair p:quesFailPairs) {
					if (!p.getFlag() && p.getFailedType() != null && p.getFailedType().startsWith(type)) {
						question = p.getTranSent();
						if (!succList.contains(question)) {
							succList.add(question);
							pairs.add(p);
						}
					}
				}
			}
		}

		if (succList.size() > 1) {
			String q1=succList.get(0), q2=q1;
			// the first one is the most similar with the original sentence
			int min, oldMin=10000;
			for (String s:succList) {
				min = StringUtils.getLevenshteinDistance(original, s);
				if (min < oldMin) {
					q1 = s;
					oldMin = min;
				}
			}
			// the second one is the most different with the first one
			int max, oldMax=0;
			// debug here! sometimes it didn't find out the most different one
			for (String s:succList) {
				max = StringUtils.getLevenshteinDistance(q1, s);
				if (max > oldMax) {
					q2 = s;
					oldMax = max;
				}
			}
			succList.remove(q1);
			if (q1!=null && q2!=null && !q1.equals(q2) && succList!=null && succList.size()>0 && succList.contains(q2))
				succList.remove(q2);
			succList.add(0, q2);
			succList.add(0, q1);
		} else if (succList.size() == 1) {
			succList.add(0, succList.get(0));
		}

		// return it anyway no matter how many entries it contains
		return succList;
	}

	private boolean runPipe(String input) {
		input = input.trim();
		boolean usePreSelector = false;

		// TODO: a better way is to check whether ' is in between letters such as "he'll", "won't"
		if (!(input.indexOf("'") == input.lastIndexOf("'")))
			input = input.replaceAll("'", "");
		input = input.replaceAll("\\(.*?\\)", "");

		SubordinateDecomposer subordDecomposer = new SubordinateDecomposer();
		CoordDecomposer coordDecomposer = new CoordDecomposer();
		ApposDecomposer apposDecomposer = new ApposDecomposer();
		SubclauseDecomposer subDecomposer = new SubclauseDecomposer();
		WhyDecomposer whyDecomposer = new WhyDecomposer();

		// pairs for declarative sentences, could be original, or decomposed.
		declSuccPairs = new ArrayList<Pair>();
		declFailPairs = new ArrayList<Pair>();
		// pairs for successfully generated questions
		quesSuccPairs = new ArrayList<Pair>();
		// pairs for not successfully generated questions
		quesFailPairs = new ArrayList<Pair>();

		// pre-processing, get the output FSC XML in a string fsc
		Preprocessor p = new Preprocessor();
		String fsc = p.getFSCbyTerms(input, true);
		input = p.getOriginalSentence();
		//log.info("\nFSC XML from preprocessing:\n");
		//log.info(fsc);

		// parsing fsc with cheap
		if (parser == null) return false;
		parser.parse(fsc);
		// the number of MRS in the list depends on
		// the option "-results=" in cheap.
		ArrayList<MRS> origMrsList = parser.getParsedMRSlist();
		ArrayList<MRS> mrxList;
		if (usePreSelector) mrxList = PreSelector.doIt(lkb, origMrsList);
		else mrxList = origMrsList;
		boolean success = parser.isSuccess();
		if (p.getNumTokens() > 15) {
			parser.releaseMemory();
		}
		if (!success) return false;

		if (mrxList == null) {
			log.warn("LKB didn't generate at all from PET input.");
			mrxList = origMrsList;
		}

		mrxList = whyDecomposer.doIt(mrxList);
		mrxList = subordDecomposer.doIt(mrxList);
		mrxList = subDecomposer.doIt(mrxList);
		mrxList = coordDecomposer.doIt(mrxList);
		mrxList = apposDecomposer.doIt(mrxList);

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
				log.info("\nGenerate from the original/decomposed sentence:\n");
				ArrayList<String> genOriSentList = lkb.getGenSentences();
				log.info(genOriSentList);
				log.info(lkb.getMaxEntScores());
				log.info("\nFrom the following MRS:\n");
				log.info(mrx);
				log.info(m);

				ArrayList<String> genOriSentFailedList = null;
				if (genOriSentList == null) {
					genOriSentFailedList = lkb.getFailedGenSentences();
				}

				if (!(genOriSentList == null && genOriSentFailedList == null)) {
					Pair pair = new Pair(input, m, genOriSentList, genOriSentFailedList);
					if (genOriSentList!=null) {
						if (pair.getOriMrs().getDecomposer().size()>0 &&
								pair.getGenOriCand() != null) {
							/* this oriMrs comes from a decomposer, the
							 * character position doesn't match the sentence
							 * any more. So we have to send it to PET and
							 * re-generate the MRS
							 */
							Preprocessor pp = new Preprocessor();
							fsc = pp.getFSCbyTerms(pair.getGenOriCand(), true);

							parser.parse(fsc);

							ArrayList<MRS> regenMrsList = parser.getParsedMRSlist();
							success = parser.isSuccess();
							if (pp.getNumTokens() > 15) {
								parser.releaseMemory();
							}
							if (!success) continue;
							if (regenMrsList!=null && regenMrsList.size()>0)
								pair.setOriMrs(regenMrsList.get(0));
						}
						declSuccPairs.add(pair);
					}
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

					// generate from transformed sentence
					lkb.sendMrxToGen(mrx);
					log.info("\nGenerated Questions:");
					ArrayList<String> genQuesList = lkb.getGenSentences();
					ArrayList<String> genQuesFailedList = null;
					if (genQuesList != null) {
						countType++;
						countNum += genQuesList.size();
						log.info(genQuesList);
						log.info(lkb.getMaxEntScores());
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
			// a second chance on failed sentences.
			if (declSuccPairs.size() == 0) declSuccPairs = declFailPairs;
			boolean debug = true;
			if (debug && lkb==null) {
				Pair pair = new Pair(input, mrxList.get(mrxList.size()-1), null, null);
				declSuccPairs.clear();
				declSuccPairs.add(pair);
			}
			// fallback
			if (declSuccPairs.size() != 0) {

				Fallback planB = new Fallback (parser, lkb, declSuccPairs);
				planB.doIt();
				ArrayList<Pair> pairs = planB.getGenSuccPairs();
				if (pairs!=null) quesSuccPairs.addAll(pairs);
				pairs = planB.getGenFailPairs();
				if (pairs!=null) quesFailPairs.addAll(pairs);

				CoordReplacer andR = new CoordReplacer (parser, lkb, declSuccPairs);
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

				WhatReplacer whatR = new WhatReplacer (parser, lkb, declSuccPairs);
				whatR.doIt();
				pairs = whatR.getGenSuccPairs();
				if (pairs!=null) quesSuccPairs.addAll(pairs);
				pairs = whatR.getGenFailPairs();
				if (pairs!=null) quesFailPairs.addAll(pairs);

				NPChunkReplacer npChunkR = new NPChunkReplacer (parser, lkb, declSuccPairs);
				npChunkR.doIt();
				pairs = npChunkR.getGenSuccPairs();
				if (pairs!=null) quesSuccPairs.addAll(pairs);
				pairs = npChunkR.getGenFailPairs();
				if (pairs!=null) quesFailPairs.addAll(pairs);

//				PPChunkReplacer ppChunkR = new PPChunkReplacer (parser, lkb, declSuccPairs);
//				ppChunkR.doIt();
//				pairs = ppChunkR.getGenSuccPairs();
//				if (pairs!=null) quesSuccPairs.addAll(pairs);
//				pairs = ppChunkR.getGenFailPairs();
//				if (pairs!=null) quesFailPairs.addAll(pairs);

				NumReplacer numR = new NumReplacer (parser, lkb, declSuccPairs);
				numR.doIt();
				pairs = numR.getGenSuccPairs();
				if (pairs!=null) quesSuccPairs.addAll(pairs);
				pairs = numR.getGenFailPairs();
				if (pairs!=null) quesFailPairs.addAll(pairs);
//
//				WhyAppender whyR = new WhyAppender (parser, lkb, declSuccPairs);
//				whyR.doIt();
//				pairs = whyR.getGenSuccPairs();
//				if (pairs!=null) quesSuccPairs.addAll(pairs);
//				pairs = whyR.getGenFailPairs();
//				if (pairs!=null) quesFailPairs.addAll(pairs);


			}
		}
		// summary
		log.info("===========Summary of Generated Questions============");
		log.info("oriSent: "+input);
		if (quesSuccPairs.size()!=0) {
			for (Pair pair:quesSuccPairs) {
				log.info("\n");
				pair.questionsRerank(ranker);
				if (pair.getGenOriCand()!=null) log.info("oriSent: "+pair.getGenOriCand());
				log.info("SentType: "+pair.getQuesMrs().getSentType());
				log.info("Decomposer: "+pair.getQuesMrs().getDecomposer());
				log.info("Question: "+pair.getGenQuesCand());
				if (ranker != null) {
					pair.printQuesRankedMap();
				}
				else
					log.info(pair.getGenQuesList());
			}
		} else {
			log.info("No questions generated.");
		}

		return true;
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

		// whether run QGSTEC2010 test
		if (prop.getProperty("runTest").equalsIgnoreCase("yes")) {
			runTest = true;
			testFileInput = new File(prop.getProperty("testFileInput"));
			testFileOutput = new File(prop.getProperty("testFileOutput"));
			QGSTEC2010processor = new QGSTEC2010(testFileInput);
		}

		// whether do fallback generation
		if (prop.getProperty("fallback").equalsIgnoreCase("yes")) {
			fallback = true;
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

		// init the cheap parser
		if (prop.getProperty("runCheapPipeline").equalsIgnoreCase("yes")) {
			System.out.println("Creating parser...");
			// Set Cheap to take FSC as input
			parser = new Cheap(true);

			if (! parser.isSuccess()) {
				log.error("cheap is not started properly.");
			}
		}

		// load language model
		if (prop.getProperty("rerank").equalsIgnoreCase("yes")) {
			System.out.println("Creating question reranker by loading language model...");
			String lmfile = prop.getProperty("lmfile");
			ranker = new Reranker(lmfile, false);
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
		System.out.println("\t1. a sentence ending with a full stop.");
		System.out.println("\t\tMrsQG generates the pre-processed FSC in XML. Then you can copy/paste this FSC into cheap to parse.");
		System.out.println("\t2. pipe: a declrative sentence ending with a full stop.");
		System.out.println("\t\tMrsQG generates a question through pipelines of PET and LKB.");
		System.out.println("\t3. mrx: an declrative MRS XML (MRX) file.");
		System.out.println("\t\tMrsQG reads this MRX and transforms it into interrogative MRX.");
		System.out.println("\t\tThen you can copy/paste the transformed MRX to LKB for generation.");
		System.out.println("\t4. lkb: an LKB command");
		System.out.println("\t\tThen MrsQG serves as a wrapper for LKB. You can talk with LKB interactively through the prompt.");
		System.out.println("\t5. cheap: a sentence");
		System.out.println("\t\tThen MrsQG serves as a wrapper for Cheap. You can talk with cheap interactively through the prompt.");
		System.out.println("\t6. pg: a sentence");
		System.out.println("\t\tThen MrsQG first parses then generates from the sentence (pg stands for parse-generation).");
		System.out.println();
	}
}
