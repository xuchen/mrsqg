/**
 * A class to hold all sentence/question pairs, including the intermediate
 * results, such as MRS representations. Generally, a MrsTransformer uses it
 * to store s/q pairs generated from original sentences, and a Fallback uses it
 * to store s/q pairs by re-generation from transformed sentences.
 */
package com.googlecode.mrsqg.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.languagemodel.Reranker;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.OpenNLP;
import com.googlecode.mrsqg.util.MapUtils;
import com.googlecode.mrsqg.util.StringUtils;

/**
 * @author Xuchen Yao
 *
 */
public class Pair {
	private static Logger log = Logger.getLogger(Pair.class);
	/** the original declarative sentence, such as "John likes Mary." */
	protected String oriSent;
	/** MRS for the original sentence */
	protected MRS oriMrs;
	/** generated original sentence list from <code>oriMrs</code> */
	protected ArrayList<String> genOriSentList;
	/** a selected best candidate from <code>genOriSentList</code> */
	protected String genOriCand;
	/** unsuccessfully generated original sentence snippet list from <code>oriMrs</code> */
	protected ArrayList<String> genOriSentFailedList;

	/** a question MRS*/
	protected MRS quesMrs;
	/** generated question list from <code>quesMrs</code> */
	protected ArrayList<String> genQuesList;
	/** generated question list with ordered (decreasing) rankings */
	protected LinkedHashMap<String, Float> quesRankedMap;
	/** a selected best candidate from <code>genQuesList</code> */
	protected String genQuesCand;
	/** unsuccessfully generated question snippet list from <code>quesMrs</code> */
	protected ArrayList<String> genQuesFailedList;

	/** transformed sentence from fallbacks, such as "John likes who?" */
	protected String tranSent;

	/** if tranSent fails to parse, still store it with a "failedType" */
	protected String failedType = null;

	private boolean flag = false;


//	/** MRS list for <code>tranSent</code> */
//	protected ArrayList<MRS> tranMrsList;
//	/** Generated sentence list from LKB, the input to LKB could either
//	 * come from <code>oriSent</code> or <code>tranSent</code>. */
//	protected ArrayList<String> genSentList;
//	/** MRS list for generated sentence list*/
//	protected ArrayList<MRS> genMrsList;
//	/** the type of question */
//	protected String quesType;
//
//	public Pair () {
//		this.genOriSentList = new ArrayList<String>();
////		this.genSentList = new ArrayList<String>();
////		this.genMrsList = new ArrayList<MRS>();
////		this.tranMrsList = new ArrayList<MRS>();
//	}
//
//	public Pair (String ori, String tranSent, String quesType) {
//		this();
//		this.oriSent = ori;
//		this.tranSent = tranSent;
//		//this.quesType = quesType;
//	}

	public Pair (String oriSent, MRS oriMrs, ArrayList<String> genOriSentList, ArrayList<String> genOriSentFailedList,
			MRS quesMrs, ArrayList<String> genQuesList, ArrayList<String> genQuesFailedList) {
		this.oriSent = oriSent;
		this.oriMrs = oriMrs;
		this.genOriSentList = genOriSentList;
		this.genOriSentFailedList = genOriSentFailedList;
		this.quesMrs = quesMrs;
		this.genQuesList = genQuesList;
		this.genQuesFailedList =genQuesFailedList;
		this.quesRankedMap = new LinkedHashMap<String, Float>();
	}

	public Pair (String oriSent, MRS oriMrs, ArrayList<String> genOriSentList, ArrayList<String> genOriSentFailedList) {
		this.oriSent = oriSent;
		this.oriMrs = oriMrs;
		this.genOriSentList = genOriSentList;
		this.genOriSentFailedList = genOriSentFailedList;
	}

	public Pair (MRS quesMrs, ArrayList<String> genQuesList, ArrayList<String> genQuesFailedList) {
		this.quesMrs = quesMrs;
		this.genQuesList = genQuesList;
		this.genQuesFailedList =genQuesFailedList;
		this.quesRankedMap = new LinkedHashMap<String, Float>();
	}

	public Pair (String tranSent, String failedType) {
		this.tranSent = tranSent;
		this.failedType = failedType;
	}

	public String getTranSent () { return this.tranSent;}
	//public void setTranMrs (ArrayList<MRS> list) { this.tranMrsList = list;}
	//public ArrayList<String> getGenSentList () {return this.genSentList;}
	public String getOriSent () {return this.oriSent;}
	public void setTranSent (String tranSent) {this.tranSent = tranSent;}
	public MRS getOriMrs () {return this.oriMrs;}
	public void setOriMrs(MRS mrs) {this.oriMrs = mrs;}
	public MRS getQuesMrs () {return this.quesMrs;}
	public ArrayList<String> getGenQuesList() {return genQuesList;}
	public LinkedHashMap<String, Float> getQuesRankedMap() {return quesRankedMap;}
	public ArrayList<String> getGenQuesFailedList() {return genQuesFailedList;}
	public String getFailedType() {return failedType;}
	public boolean getFlag() {return this.flag;}
	public void setFlag(boolean flag) {this.flag = flag;}

	public String getGenOriCand() {
		if (genOriCand != null) return genOriCand;
		if (genOriSentList == null) return null;
		ArrayList<String> shortest = StringUtils.getShortest(genOriSentList);

		if (oriSent == null) {
			genOriCand = shortest.get(0);
		} else {
			// find out the one that's most similar to oriSent
			int lowest, oldLowest = 10000;
			for (String s:shortest) {
				lowest = StringUtils.getLevenshteinDistance(oriSent, s);
				if (lowest < oldLowest) {
					genOriCand = s;
					oldLowest = lowest;
				}
			}
		}

		return genOriCand;
	}

	/**
	 * re-rank all question candidates with a language model
	 */
	public void questionsRerank(Reranker ranker) {
		if (genQuesList == null) return;
		if (ranker == null) return;
		String[] tokens;
		float rank;
		for (String oriSent:genQuesList) {
			tokens = OpenNLP.tokenize(oriSent);
			tokens = StringUtils.lowerCaseList(tokens);
			rank = ranker.rank(tokens);
			// normalize with sentence length, plus <s> and </s>
			rank /= (tokens.length+2);
			// rank is usually between 0 and -10
			rank += 10;
			this.quesRankedMap.put(oriSent, rank);
		}
		this.quesRankedMap = MapUtils.sortByDecreasingValue(this.quesRankedMap);
		Iterator<String> ite = this.quesRankedMap.keySet().iterator();
		while(ite.hasNext()) {
			this.genQuesCand = ite.next();
			break;
		}
	}

	// Post selection
	/*
	 * TODO: LKB tend to have a favor of "topicalization" in the front of the generation list:
	 * Why is dating of prehistoric materials particularly crucial to the enterprise?
	 * -> To the enterprise, why is dating of prehistoric materials particularly crucial?
	 * should find a way to use this pattern.
	 *
	 * TODO:
	 * pg: This is the water that is red.
	 * [This is the water, that is red., This is the water, who is red.,
	 * This is the water, which is red., This is the water that is red.,
	 * wThis is the water who is red., This is the water which is red.]
	 */
	public String getGenQuesCand() {
		if (genQuesCand != null) return genQuesCand;
		if (genQuesList == null) return null;
		ArrayList<String> shortest;
		String sentType = this.quesMrs.getSentType();
		ArrayList<String> preferred = new ArrayList<String>();

		if (sentType.startsWith("HOW ")) sentType = "HOW";

		if (!sentType.equals("Y/N")) {
			// prefer the one which has the question word in the front
			// as well as having a ? at the end
			for (String s:genQuesList) {
				if (//s.toUpperCase().startsWith(sentType) &&
						s.endsWith("?"))
					preferred.add(s);
			}
		}

		if (preferred.size() != 0) shortest = preferred;
		else shortest = genQuesList;

		if (sentType.equals("Y/N")) {
			// in ["he likes cats?", "Does he like cats?"]
			// prefer the longer one
			shortest = StringUtils.getLongest(shortest);
		} else {
			// in ["who likes cats?", "who does like cats?"]
			// prefer the shorter one
			shortest = StringUtils.getShortest(shortest);
		}

		if (oriSent == null) {
			genQuesCand = shortest.get(0);
		} else {
			// find out the one that's most different to oriSent
			// to increase the evaluation grade for variety
			// new strategy: find out most similar one

			int lowest, oldLowest = 10000;
			for (String s:shortest) {

				// some questions don't have the correct question word
				// e.g. WHICH -> 'what place' / 'which place'
				if (!sentType.equals("Y/N") && !s.toUpperCase().contains(sentType)) continue;

				lowest = StringUtils.getLevenshteinDistance(oriSent, s);
				if (lowest < oldLowest) {
					genQuesCand = s;
					oldLowest = lowest;
				}
			}
		}

		return genQuesCand;
	}

	public void printQuesRankedMap() {
		Iterator<String> ite = this.quesRankedMap.keySet().iterator();
		String ques;
		float grade;
		while(ite.hasNext()) {
			ques = ite.next();
			grade = quesRankedMap.get(ques);
			log.info(grade+": "+ques);
		}
	}
}
