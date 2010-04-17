/**
 * 
 */
package com.googlecode.mrsqg.evaluation;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Xuchen Yao
 *
 */
public class Instance {
	private static Logger log = Logger.getLogger(Instance.class);
	
	/**  <instance id="1"> */
	protected String idNum;
	/** <id>OpenLearn</id> */
	protected String idSource;
	/** <source>A103_3</source> */
	protected String source;
	/** <text>The view that ... </text>*/
	protected String text;
	/**
	 * <question type="how many"></question>
	 */
	protected ArrayList<String> questionTypeList;
	
	/**
	 * Generated question from <code>text</code> according to <code>questionTypeList</code>.
	 */
	protected ArrayList<String> genQuestionList;
	
	public void setIdNum (String idNum) {this.idNum = idNum;}
	public void setIdSource (String idSource) {this.idSource= idSource;}
	public void setSource (String source) {this.source = source;}
	public void setText (String text) {this.text = text;}
	public ArrayList<String> getQuestionTypeList () {return this.questionTypeList;}
	public ArrayList<String> getGenQuestionList () {return this.genQuestionList;}
	public void addQuestionType (String type) {this.questionTypeList.add(type);}
	public void addGenQuestion (String question) {this.genQuestionList.add(question);}
	public String getText () {return this.text;}
	public String getIdNum () {return this.idNum;}
	public String getIdSource () {return this.idSource;}
	public String getSource () {return this.source;}
	
	public Instance () {
		this.questionTypeList = new ArrayList<String>();
		this.genQuestionList = new ArrayList<String>();
	}

}
