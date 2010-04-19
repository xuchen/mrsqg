/**
 * 
 */
package com.googlecode.mrsqg.evaluation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

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
	
	private ArrayList<String> candidatesList;
	
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
	public void addToCandidatesList (String s) {this.candidatesList.add(s);}
	
	public Instance () {
		this.questionTypeList = new ArrayList<String>();
		this.genQuestionList = new ArrayList<String>();
		this.candidatesList = new ArrayList<String>();
	}
	
	public void toXML(OutputStream os) {
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);

		of.setIndenting(true);
		of.setIndent(1);
		//of.setLineWidth(72);

//		FileOutputStream fos = null;
//		try {
//			fos = new FileOutputStream("");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		XMLSerializer serializer = new XMLSerializer(os,of);
		// SAX2.0 ContentHandler.
		ContentHandler hd;
		try {
			hd = serializer.asContentHandler();
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();

			String tmp;

				// <instance id="1">
				atts.clear();
				atts.addAttribute("", "", "id", "CDATA", idNum);
				hd.startElement("", "", "instance", atts);
				
				// <id>OpenLearn</id>
				atts.clear();
				hd.startElement("", "", "id", atts);
				tmp = idSource;
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "id");
				
				// <source>A103_3</source>
				atts.clear();
				hd.startElement("", "", "source", atts);
				tmp = source;
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "source");
				
				// <text>...</text>
				atts.clear();
				hd.startElement("", "", "text", atts);
				tmp = text;
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "text");
				
				// <question type="how many">
			    // </question>

				String questionType, question;
				for (int i=0; i<questionTypeList.size(); i++) {
					questionType = questionTypeList.get(i);
					atts.clear();
					atts.addAttribute("", "", "id", "CDATA", questionType);
					hd.startElement("", "", "question", atts);
					if (i<genQuestionList.size()) {
						question = genQuestionList.get(i);
						if (question != null && question.length() != 0)
							hd.characters(question.toCharArray(), 0, question.length());
					}
					hd.endElement("", "", "question");
					if (i%2==1 && candidatesList!=null && i/2<candidatesList.size()) {
						atts.clear();
						hd.startElement("", "", "questions", atts);
						question = candidatesList.get(i/2);
						hd.characters(question.toCharArray(), 0, question.length());
						hd.endElement("", "", "questions");
					}
				}
				
				
				hd.endElement("", "", "instance");

			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
	}


}
