package com.googlecode.mrsqg.mrs;

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

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import com.googlecode.mrsqg.Preprocessor;


public class MRS {
	
	/*
	 * !!! WARNING !!!
	 * Any new field added to this class must also be added to the copy constructor. 
	 */
	
	private static Logger log = Logger.getLogger(MRS.class);
	// h1
	private String ltop = "";
	// 1
	private String label_vid = "";
	// 2
	private String index = "";
	// 2
	private String index_vid = "";
	private ArrayList <ElementaryPredication> eps;
	private ArrayList<HCONS> hcons;
	private MrsParser parser = new MrsParser();
	
	public String getLTOP() {return ltop;}
	public String getLabelVid() {return label_vid;}
	public String getIndex() {return index;}
	public String getIndexVid() {return index_vid;} 
	public ArrayList <ElementaryPredication> getEps() {return eps;}
	public ArrayList<HCONS> getHcons() {return hcons;}

	public MRS() {
		hcons = new ArrayList<HCONS>();
		eps = new ArrayList<ElementaryPredication>();
	}
	
	public MRS(String file) {
		this();
		parser.parse(file);
	}
	
	/**
	* Copy constructor.
	*/
	public MRS(MRS old) {
		if (old == null) return;
		this.ltop = old.getLTOP();
		this.label_vid = old.getLabelVid();
		this.index = old.getIndex();
		this.index_vid = old.getIndexVid();
		this.eps = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:old.getEps()) {
			this.eps.add(new ElementaryPredication(ep));
		}
		this.hcons = new ArrayList<HCONS>();
		for (HCONS h:old.getHcons()) {
			this.hcons.add(new HCONS(h));
		}
	}

	
	private class HCONS {
//		;;; <!ELEMENT hcons (hi, lo)>
//		;;; <!ATTLIST hcons 
//		;;;          hreln (qeq|lheq|outscopes) #REQUIRED >
//		;;;
//		;;; <!ELEMENT hi (var)>
//		;;; <!ELEMENT lo (label|var)>

		
		/*
		 * !!! WARNING !!!
		 * Any new field added to this class must also be added to the copy constructor. 
		 */
		
		private String rel = null;
		private String hi = null;
		private String lo = null;
		private Var hi_var = null;
		
		// could be either var or label
		private Var lo_var = null;
		private String lo_label = null;
		
		public String getRel() {return rel;}
		public String getHi() {return hi;}
		public String getLo() {return lo;}
		public Var getHiVar() {return hi_var;}
		public Var getLoVar() {return lo_var;}
		public String getLoLabel() {return lo_label;}
		
		/**
		* Copy constructor.
		*/
		public HCONS(HCONS old) {
			if (old == null) return;
			this.rel = old.getRel();
			this.hi = old.getHi();
			this.lo = old.getLo();
			this.hi_var = new Var(old.getHiVar());
			this.lo_var = new Var(old.getLoVar());
			this.lo_label = old.getLoLabel();
		}
		
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
		
		public void parse(String file) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				MrsParser handler = new MrsParser();
				xr.setContentHandler(handler);
				xr.setErrorHandler(handler);
				

				FileReader r = new FileReader(file);
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
				eps.add(e);
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
	
	/**
	 * Return all ElementaryPredication starting from cfrom and ending to cto.
	 */
	public ArrayList<ElementaryPredication> getEPS (int cfrom, int cto) {
		ArrayList<ElementaryPredication> epsList= new ArrayList<ElementaryPredication>();
		
		for (ElementaryPredication ep:this.eps) {
			if (ep.getCfrom()==cfrom && ep.getCto()==cto) {
				epsList.add(ep);
			}
		}
		
		if (epsList.size() == 0) {
			log.error(String.format("EPS(c%d-c%d) not found.", cfrom, cto));
		}
		
		return epsList;
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
			for (ElementaryPredication e: eps) {
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
		for(String file:args) {
			m.parser.parse(file);
			System.out.println("done");
			m.printXML();
		}
	}
}