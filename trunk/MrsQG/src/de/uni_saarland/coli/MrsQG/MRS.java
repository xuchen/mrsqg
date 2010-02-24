package de.uni_saarland.coli.MrsQG;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.xml.serialize.OutputFormat;

public class MRS {
	
	public String ltop = "";
	public String index = "";
	public ArrayList <ElementaryPredication> ep;
	public ArrayList<HCONS> hcons;
	private MrsParser parser = new MrsParser();

	public MRS() {
		hcons = new ArrayList<HCONS>();
		ep = new ArrayList<ElementaryPredication>();
	}
	
	private class HCONS {
		public String rel = null;
		public String hi = null;
		public String lo = null;
		
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
				} else if (parent.equals("ep")) {
					// label for <ep>
					currentEP.processStartElement(qName, atts);
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
				} else if (parent.equals("lo")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
					h.lo = sort+vid;
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
	}
	
	public static void main(String args[]) 
	throws Exception {
		MRS m = new MRS();
		m.parser.main(args);
		System.out.println("done");
	}
}