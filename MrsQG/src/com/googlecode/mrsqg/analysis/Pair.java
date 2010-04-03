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
	/** transformed sentence from fallbacks, such as "John likes who?" */
	protected String tranSent;
	/** MRS list for <code>tranSent</code> */
	protected ArrayList<MRS> tranMrsList;
	/** Generated sentence list from LKB, the input to LKB could either 
	 * come from <code>oriSent</code> or <code>tranSent</code>. */
	protected ArrayList<String> genSentList;
	/** MRS list for generated sentence list*/
	protected ArrayList<MRS> genMrsList;
	/** the type of question */
	protected String quesType;
	
	public Pair () {
		this.genSentList = new ArrayList<String>();
		this.genMrsList = new ArrayList<MRS>();
		this.tranMrsList = new ArrayList<MRS>();
	}
	
	public Pair (String ori, String tranSent, String quesType) {
		this();
		this.oriSent = ori;
		this.tranSent = tranSent;
		this.quesType = quesType;
	}
	
	public String getTranSent () { return this.tranSent;}
	public void setTranMrs (ArrayList<MRS> list) { this.tranMrsList = list;}
	public ArrayList<String> getGenSentList () {return this.genSentList;}
	public String getOriSent () {return this.oriSent;}
}
