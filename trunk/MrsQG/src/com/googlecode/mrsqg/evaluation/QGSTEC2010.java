/**
 * 
 */
package com.googlecode.mrsqg.evaluation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Xuchen Yao
 *
 */
public class QGSTEC2010 {

	private static Logger log = Logger.getLogger(QGSTEC2010.class);
	
	ArrayList<Instance> instanceList;
	
	private Instance currentInstance;
	
	private TestXmlParser parser;
	
	public QGSTEC2010 () {
		instanceList = new ArrayList<Instance>();
		parser = new TestXmlParser();
	}
	
	public QGSTEC2010 (File file) {
		this();
		this.parser.parse(file);
	}
	
	public ArrayList<Instance> getInstanceList() {
		return instanceList;
	}
	
	private class TestXmlParser extends DefaultHandler {
		
		private Stack<String> stack;
		private StringBuilder chars;

		public TestXmlParser () {
			super();
			this.stack = new Stack<String>();
			this.chars = new StringBuilder();
		}
		
		public void parse(File file) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				TestXmlParser handler = new TestXmlParser();
				xr.setContentHandler(handler);
				xr.setErrorHandler(handler);
				
				FileReader r = new FileReader(file);
				xr.parse(new InputSource(r));
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		}
		
		public void parseString(String str) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				TestXmlParser handler = new TestXmlParser();
				xr.setContentHandler(handler);
				xr.setErrorHandler(handler);
				
				StringReader r = new StringReader(str);
				xr.parse(new InputSource(r));
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		}
		
		public void startDocument ()
	    {
			System.out.println("Start document");
	    }
		
		public void endDocument ()
	    {
			System.out.println("End document");
	    }
	
		public void startElement (String uri, String name,
				String qName, Attributes atts)
		{
			String idNum, questionType;
			
			if (qName.equals("dataset")) {
				// if stack is not empty, then error
				if (stack.empty() == false) {
					log.error("Error, non-empty stack: " +
							"<dataset> shouldn't have parent element");
				}
			} else if (qName.equals("instance")) {
				// <instance id="1">
				idNum = atts.getValue("id");

				currentInstance = new Instance();
				
				currentInstance.setIdNum(idNum);

			} else if (qName.equals("question")) {
				// <question type="how many">
				questionType = atts.getValue("type");
				currentInstance.addQuestionType(questionType);
			} else {
				log.error("Unknown element "+qName);
			}
			chars = new StringBuilder();
			stack.push(qName);
		}
		
		public void endElement (String uri, String name, String qName)
		{
			String idSource, source, text;
			if (qName.equals("instance")) {
				if (currentInstance != null)
					instanceList.add(currentInstance);
				else
					log.error("currentInstance shouldn't be none!");
			}else if (qName.equals("id")) {
				// <id>OpenLearn</id>
				idSource = chars.toString();
				currentInstance.setIdSource(idSource);
			} else if (qName.equals("source")) {
				// <source>A103_3</source>
				source = chars.toString();
				currentInstance.setSource(source);
			} else if (qName.equals("text")) {
				// <text>...</text>
				text = chars.toString();
				currentInstance.setText(text);
			} 
			stack.pop();
		}
	
		public void characters (char ch[], int start, int length)
		{
			chars.append(ch, start, length);
		}

	}
	
	public void toXML(OutputStream os) {
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);

		of.setIndenting(true);
		of.setIndent(1);

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
			// <dataset>
			hd.startElement("", "", "dataset", atts);
			String tmp;
			for (Instance ins : instanceList) {
				// <instance id="1">
				atts.clear();
				atts.addAttribute("", "", "id", "CDATA", ins.getIdNum());
				hd.startElement("", "", "instance", atts);
				
				// <id>OpenLearn</id>
				atts.clear();
				hd.startElement("", "", "id", atts);
				tmp = ins.getIdSource();
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "id");
				
				// <source>A103_3</source>
				atts.clear();
				hd.startElement("", "", "source", atts);
				tmp = ins.getSource();
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "source");
				
				// <text>...</text>
				atts.clear();
				hd.startElement("", "", "text", atts);
				tmp = ins.getText();
				hd.characters(tmp.toCharArray(), 0, tmp.length());
				hd.endElement("", "", "text");
				
				// <question type="how many">
			    // </question>
				ArrayList<String> qTypeList = ins.getQuestionTypeList();
				ArrayList<String> qList = ins.getGenQuestionList();
				String questionType, question;
				for (int i=0; i<qTypeList.size(); i++) {
					questionType = qTypeList.get(i);
					atts.clear();
					atts.addAttribute("", "", "id", "CDATA", questionType);
					hd.startElement("", "", "question", atts);
					if (i<qList.size()) {
						question = qList.get(i);
						hd.characters(question.toCharArray(), 0, tmp.length());
					}
					hd.endElement("", "", "question");
				}
				
				hd.endElement("", "", "instance");
			}
			hd.endElement("", "", "dataset");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * This method parses a QGSTEC2010 Test document in XML. 
	 * 
	 * @param file an XML file
	 */
	public void parse(File file) {
		this.parser.parse(file);
	}
	
	public static void main(String[] args) {
		File file = new File("/home/xcyao/delphin/mrs.xml/QuestionsFromSentences.Test.2010.good.xml");
		//File file = new File("/tmp/t.xml");
		//File file = new File("/home/xcyao/delphin/mrs.xml/QuestionsFromSentences.Development.xml");
		QGSTEC2010 q = new QGSTEC2010(file);
		q.toXML(System.out);
	}

}
