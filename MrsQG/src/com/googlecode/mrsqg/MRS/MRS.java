package com.googlecode.mrsqg.MRS;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;


public class MRS {
	
	// h1
	public String ltop = "";
	// 1
	public String label_vid = "";
	// 2
	public String index = "";
	// 2
	public String index_vid = "";
	public ArrayList <ElementaryPredication> ep;
	public ArrayList<HCONS> hcons;
	private MrsParser parser = new MrsParser();

	public MRS() {
		hcons = new ArrayList<HCONS>();
		ep = new ArrayList<ElementaryPredication>();
	}
	
	private class HCONS {
//		;;; <!ELEMENT hcons (hi, lo)>
//		;;; <!ATTLIST hcons 
//		;;;          hreln (qeq|lheq|outscopes) #REQUIRED >
//		;;;
//		;;; <!ELEMENT hi (var)>
//		;;; <!ELEMENT lo (label|var)>

		
		public String rel = null;
		public String hi = null;
		public String lo = null;
		public Var hi_var = null;
		
		// could be either var or label
		public Var lo_var = null;
		public String lo_label = null;
		
		public HCONS(String rel) {
			this.rel = rel;
		}
		
		public boolean checkValid() {
			if (rel != null && hi != null && lo != null) {
				return true;
			} else {
				return false;
			}
		}
		
		public void serializeXML (ContentHandler hd) {
			//<hcons hreln='qeq'><hi><var vid='4' sort='h'></var></hi><lo><var vid='7' sort='h'></var></lo></hcons>
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute("", "", "hreln", "CDATA", rel);
			try {
				// <hcons>
				hd.startElement("", "", "hcons", atts);
				// <hi>
				atts.clear();
				hd.startElement("", "", "hi", atts);
				hi_var.serializeXML(hd);
				hd.endElement("", "", "hi");
				// <lo>
				atts.clear();
				hd.startElement("", "", "lo", atts);
				if (lo_var != null) {
					lo_var.serializeXML(hd);
				} else if (lo_label != null) {
					// <label>
					atts.clear();
					atts.addAttribute("", "", "vid", "CDATA", lo_label);
					hd.startElement("", "", "label", atts);
					hd.endElement("", "", "label");
				} else {
					
					System.err.println("Error, <lo> must have either <lo> or <label>");
				}
				hd.endElement("", "", "lo");
				
				hd.endElement("", "", "hcons");
			} catch (SAXException e) {
				e.printStackTrace();
			}
			
		}
	}

	private class MrsParser extends DefaultHandler {
		
		private Stack<String> stack;
		private StringBuilder chars;
		// whether we are processing in an <ep> element
		private boolean inEP = false;
		private ElementaryPredication currentEP = null;
		
		public MrsParser () {
			super();
			this.stack = new Stack<String>();
			this.chars = new StringBuilder();
		}
		
		public void main(String args[]) 
			throws Exception {
			XMLReader xr = XMLReaderFactory.createXMLReader();
			MrsParser handler = new MrsParser();
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			
			// Parse each file provided on the
			// command line.
			for (int i = 0; i < args.length; i++) {
			    FileReader r = new FileReader(args[i]);
			    xr.parse(new InputSource(r));
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
			String vid;
			String parent;
			
			if (qName.equals("mrs")) {
				// if stack is not empty, then error
				if (stack.empty() == false) {
					System.err.println("Error, non-empty stack: " +
							"<mrs> shouldn't have parent element");
				}
			} else if (qName.equals("label")) {
				parent = stack.peek();
				vid = atts.getValue("vid");
				
				// top element, indicating the LTOP of MRS
				if (parent.equals("mrs")) {
					ltop = "h"+vid;
					label_vid = vid;
				} else if (parent.equals("ep")) {
					// label for <ep>
					currentEP.processStartElement(qName, atts);
				} else if (parent.equals("lo")) {
					HCONS h = hcons.get(hcons.size()-1);
					h.lo_label = atts.getValue("vid");
					h.lo = h.lo_label;
					System.err.println("Warning: <label> inisde <lo>. " +
							"not in sample. check the code!");
				} else {
					System.err.println("file format error: unknown" +
							"element label");
				}
			} else if (qName.equals("var")) {
				parent = stack.peek();
				vid = atts.getValue("vid");
				
				// top element, indicating the INDEX of MRS
				if (parent.equals("mrs")) {
					index = vid;
					index_vid = vid;
				} else if (parent.equals("fvpair")) {
					// label for <fvpair>
					if (inEP) {
						currentEP.processStartElement(qName, atts);
					} else {
						System.err.println("error: <fvpair> outside <ep>");
					}
				} else if (parent.equals("hi")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
					h.hi = sort+vid;
					h.hi_var = new Var(atts);
				} else if (parent.equals("lo")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
					h.lo = sort+vid;
					h.lo_var = new Var(atts);
				} else {
					System.err.println("file format error: unknown" +
							"element var");
				}
			} else if (qName.equals("hcons")) {
//				;;; <!ELEMENT hcons (hi, lo)>
//				;;; <!ATTLIST hcons 
//				;;;          hreln (qeq|lheq|outscopes) #REQUIRED >
//				;;;
//				;;; <!ELEMENT hi (var)>
//				;;; <!ELEMENT lo (label|var)>

				String hreln = atts.getValue("hreln");
				if (hreln.equals("lheq") || hreln.equals("outscopes")) {
					// no such situation in sample files, need to complete
					// this part once met
					System.err.println("Manually check the code and complete it!");
				}
				HCONS hcon = new HCONS(hreln);
				hcons.add(hcon);
			} else if (qName.equals("ep")) {
				inEP = true;
				ElementaryPredication e = new ElementaryPredication();
				ep.add(e);
				currentEP = e;
				currentEP.processStartElement(qName, atts);
			} else if (inEP == true) {
				currentEP.processStartElement(qName, atts);
			} else if (qName.equals("hi") || qName.equals("lo")) {
			} else {
				System.err.println("Unknown element "+qName);
			}
			chars = new StringBuilder();
			stack.push(qName);
		}
		
		public void endElement (String uri, String name, String qName)
		{
			if (qName.equals("mrs")) {

			} else if (qName.equals("hcons")) {
				HCONS h = hcons.get(hcons.size()-1);
				if (h.checkValid() == false) {
					System.err.println("HCONS read error!");
				}
			} else if (qName.equals("ep")) {
				inEP = false;
				currentEP = null;
			} else if (inEP) {
				currentEP.processEndElement(qName, chars.toString());
			}
			stack.pop();
		}
	
		public void characters (char ch[], int start, int length)
		{
			chars.append(ch, start, length);
		}

	}
	
	public void printXML() {
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setIndenting(true);
//		FileOutputStream fos = null;
//		try {
//			fos = new FileOutputStream("");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		XMLSerializer serializer = new XMLSerializer(System.out,of);
		// SAX2.0 ContentHandler.
		ContentHandler hd;
		try {
			hd = serializer.asContentHandler();
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			// <mrs>
			hd.startElement("", "", "mrs", atts);
			// <label  vid='1'/>
			atts.clear();
			atts.addAttribute("", "", "vid", "CDATA", label_vid);
			hd.startElement("", "", "label", atts);
			hd.endElement("", "", "label");
			// <var vid='2'/>
			atts.clear();
			atts.addAttribute("", "", "vid", "CDATA", index_vid);
			hd.startElement("", "", "var", atts);
			hd.endElement("", "", "var");
			// <ep>
			for (ElementaryPredication e: ep) {
				e.serializeXML(hd);
			}
			// <hcons>
			for (HCONS h: hcons) {
				h.serializeXML(hd);
			}
			
			hd.endElement("", "", "mrs");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String args[]) 
	throws Exception {
		MRS m = new MRS();
		m.parser.main(args);
		System.out.println("done");
		m.printXML();
	}
}