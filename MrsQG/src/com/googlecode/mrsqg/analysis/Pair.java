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
	/** MaxEnt scores from LOGON after generation. */
	protected double[] maxEntScores;
	/** Normalized sentence log-likelihood from the language model*/
	protected double[] lmScores = null;
	/** Overall score, weighted between maxEntScores and lmScores */
	protected double[] overallScores;
	/** generated question list with ordered (decreasing) rankings */
	protected LinkedHashMap<Integer, Double> quesRankedMap;
	/** a selected best candidate from <code>genQuesList</code> */
	protected String genQuesCand;
	/** unsuccessfully generated question snippet list from <code>quesMrs</code> */
	protected ArrayList<String> genQuesFailedList;

	/** transformed sentence from fallbacks, such as "John likes who?" */
	protected String tranSent;

	/** if tranSent fails to parse, still store it with a "failedType" */
	protected String failedType = null;

	private boolean flag = false;


	public Pair (String oriSent, MRS oriMrs, ArrayList<String> genOriSentList, ArrayList<String> genOriSentFailedList,
			MRS quesMrs, ArrayList<String> genQuesList, double[] maxEntScores, ArrayList<String> genQuesFailedList) {
		this.oriSent = oriSent;
		this.oriMrs = oriMrs;
		this.genOriSentList = genOriSentList;
		this.genOriSentFailedList = genOriSentFailedList;
		this.quesMrs = quesMrs;
		this.genQuesList = genQuesList;
		this.genQuesFailedList =genQuesFailedList;
		this.maxEntScores = maxEntScores;
		this.quesRankedMap = new LinkedHashMap<Integer, Double>();
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
		this.quesRankedMap = new LinkedHashMap<Integer, Double>();
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
	public LinkedHashMap<Integer, Double> getQuesRankedMap() {return quesRankedMap;}
	public ArrayList<String> getGenQuesFailedList() {return genQuesFailedList;}
	public String getFailedType() {return failedType;}
	public boolean getFlag() {return this.flag;}
	public void setFlag(boolean flag) {this.flag = flag;}

	public String getGenOriCand() {
		if (genOriCand != null) return genOriCand;
		if (genOriSentList == null) return null;
		ArrayList<String> shortest = StringUtils.getShortest(genOriSentList);

		/*
		 * Though we might have a MaxEnt model from LOGON here, we don't
		 * want to use it: statistics might favor different PP attachment.
		 * What we need is just simply the most similar to the original one.
		 */
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
	 * Re-rank all question candidates with a language model.
	 * If LOGON is used, then the re-ranked scores are combined with
	 * the MaxEnt score to give an overall score.
	 */
	public void questionsRerank(Reranker ranker) {
		if (genQuesList == null) return;
		if (ranker == null && maxEntScores == null) return;
		String[] tokens;
		double rank;
		String oriSent;
		if (ranker != null) {
			lmScores = new double[genQuesList.size()];
			for (int i=0; i<genQuesList.size(); i++) {
				oriSent = genQuesList.get(i);
				tokens = OpenNLP.tokenize(oriSent);
				tokens = StringUtils.lowerCaseList(tokens);
				rank = ranker.rank(tokens);
				lmScores[i] = rank;
			}
		}

		if (maxEntScores != null)
			maxEntScores = Reranker.normalize(maxEntScores);
		if (lmScores != null)
			lmScores = Reranker.normalize(lmScores);

		overallScores = Reranker.Fbeta(maxEntScores, lmScores, 1);
		if (overallScores == null) return;

		for (int i=0; i<overallScores.length; i++) {
			this.quesRankedMap.put(i, overallScores[i]);
		}

		this.quesRankedMap = MapUtils.sortByDecreasingValue(this.quesRankedMap);
		Iterator<Integer> ite = this.quesRankedMap.keySet().iterator();
		while(ite.hasNext()) {
			this.genQuesCand = genQuesList.get(ite.next());
			break;
		}
	}

	// Post selection
	/**
	 * The old rule-based way. Now upgraded to statistics.
	 */
	public String getGenQuesCand() {
		// if a ranker exists, genQuesCand will be already set,
		// so the code doesn't go further.
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
		Iterator<Integer> ite = this.quesRankedMap.keySet().iterator();
		String ques;
		double grade;
		int i;
		String gradeME, gradeLM;
		while(ite.hasNext()) {
			i = ite.next();
			ques = genQuesList.get(i);
			grade = quesRankedMap.get(i);
			gradeME = maxEntScores==null?"nil":String.format(".2f", maxEntScores[i]);
			gradeLM = lmScores==null?"nil":String.format(".2f", lmScores[i]);
			log.info(grade+"(ME:"+gradeME+"|LM:"+gradeLM+"): "+ques);
		}
	}
}
