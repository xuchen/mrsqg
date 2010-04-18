/**
 * A class to hold all sentence/question pairs, including the intermediate 
 * results, such as MRS representations. Generally, a MrsTransformer uses it
 * to store s/q pairs generated from original sentences, and a Fallback uses it
 * to store s/q pairs by re-generation from transformed sentences.
 */
package com.googlecode.mrsqg.analysis;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.MRS;
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
	/** a selected best candidate from <code>genQuesList</code> */
	protected String genQuesCand;
	/** unsuccessfully generated question snippet list from <code>quesMrs</code> */
	protected ArrayList<String> genQuesFailedList;
	
	/** transformed sentence from fallbacks, such as "John likes who?" */
	protected String tranSent;
	
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
	public ArrayList<String> getGenQuesFailedList() {return genQuesFailedList;}
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
				if (lowest < oldLowest)
					genOriCand = s;
			}
		}
			
		return genOriCand;
	}
	
	// Post selection
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
				if (s.toUpperCase().startsWith(sentType) &&
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
			int lowest, oldLowest = 0;
			for (String s:shortest) {
				
				// some questions don't have the correct question word
				// e.g. WHICH -> 'what place' / 'which place'
				if (!s.toUpperCase().contains(sentType)) continue;

				lowest = StringUtils.getLevenshteinDistance(oriSent, s);
				if (lowest > oldLowest)
					genQuesCand = s;
			}
		}
			
		return genQuesCand;
	}
}
