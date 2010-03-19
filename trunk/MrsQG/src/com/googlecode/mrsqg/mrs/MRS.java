package com.googlecode.mrsqg.mrs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	// e2
	private String index = "";
	// 2
	private String index_vid = "";
	// the type of sentence, e.g. PROP, WHEN, WHERE, etc
	private String sent_force = "PROP";
	
	private ArrayList <ElementaryPredication> eps;
	private ArrayList<HCONS> hcons;
	private MrsParser parser = new MrsParser();
	
	public String getLTOP() {return ltop;}
	public String getLabelVid() {return label_vid;}
	public String getIndex() {return index;}
	public String getIndexVid() {return index_vid;} 
	public String getSentForce() {return sent_force;}
	public ArrayList <ElementaryPredication> getEps() {return eps;}
	public ArrayList<HCONS> getHcons() {return hcons;}
	
	public void setSentForce (String sentForce) {sent_force = sentForce;};

	
	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("\n");
		res.append("SentForce: "+sent_force+"\n");
		// LTOP: h1
		res.append("LTOP: "+ltop+"\n");
		// INDEX: e2 [ e SF: PROP-OR-QUES TENSE: PRES MOOD: INDICATIVE PROG: - PERF: - ]
		res.append("INDEX: "+index+"\n");
		res.append("RELS: <\n");
		for (ElementaryPredication ep: eps) {
			res.append(ep);
			res.append("\n");
		}
		res.append(">\n");
		res.append("HCONS: < ");
		for (HCONS h: hcons) {
			res.append(h+" ");
		}
		res.append(">\n");
		return res.toString();
	}
	
	public MRS() {
		hcons = new ArrayList<HCONS>();
		eps = new ArrayList<ElementaryPredication>();
	}
	
	public MRS(File file) {
		this();
		parse(file);
	}
	
	public MRS(String mrx) {
		this();
		parseString(mrx);
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
		this.sent_force = old.getSentForce();
		this.eps = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:old.getEps()) {
			this.eps.add(new ElementaryPredication(ep));
		}
		this.hcons = new ArrayList<HCONS>();
		for (HCONS h:old.getHcons()) {
			this.hcons.add(new HCONS(h));
		}
		this.buildCoref();
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
		
		public void parse(File file) {
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
		
		public void parseString(String str) {
			try {
				XMLReader xr = XMLReaderFactory.createXMLReader();
				MrsParser handler = new MrsParser();
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
			//System.out.println("Start document");
	    }
		
		public void endDocument ()
	    {
			//System.out.println("End document");
	    }
	
		public void startElement (String uri, String name,
				String qName, Attributes atts)
		{
			String vid;
			String parent;
			
			if (qName.equals("mrs")) {
				// if stack is not empty, then error
				if (stack.empty() == false) {
					log.error("Error, non-empty stack: " +
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
					h.setLoLabel(atts.getValue("vid"));
					h.setLo(atts.getValue("vid"));
					log.error("Warning: <label> inisde <lo>. " +
							"not in sample. check the code!");
				} else {
					log.error("file format error: unknown" +
							"element label");
				}
			} else if (qName.equals("var")) {
				parent = stack.peek();
				vid = atts.getValue("vid");
				
				// top element, indicating the INDEX of MRS
				if (parent.equals("mrs")) {
					index = "e"+vid;
					index_vid = vid;
				} else if (parent.equals("fvpair")) {
					// label for <fvpair>
					if (inEP) {
						currentEP.processStartElement(qName, atts);
					} else {
						log.error("error: <fvpair> outside <ep>");
					}
				} else if (parent.equals("hi")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
					h.setHi(sort+vid);
					h.setHiVar(new Var(atts));
				} else if (parent.equals("lo")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
					h.setLo(sort+vid);
					h.setLoVar(new Var(atts));
				} else {
					log.error("file format error: unknown" +
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
					log.error("Manually check the code and complete it!");
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
				log.error("Unknown element "+qName);
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
					log.error("HCONS read error!");
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
	
	/**
	 * Get the EP before cfrom and cto. This method is mainly used to
	 * find out the preposition before a time/location term.
	 * For instance, "in Germany", one EP is  _IN_P_REL ("in"),
	 * the other is PROPER_Q_REL ("Germany"), feeding cfrom and cto with 
	 * those of PROPER_Q_REL returns the _IN_P_TEMP_REL EP.
	 * 
	 */
	public ElementaryPredication getEPbefore (int cfrom, int cto) {
		ElementaryPredication ret = null;
		ElementaryPredication next = null;
		
		// In a well-formed MRS, all EPs are lined up according to 
		// their position in the sentence
		for (ElementaryPredication ep:this.eps) {
			if (ep.getCfrom()==cfrom && ep.getCto()==cto) {
				next = ep;
				break;
			}
			ret = ep;
		}
		
		// extra safety
		if (ret!=null && next!= null && ret.getCto() >= next.getCfrom()) {
			log.error("ep1 should be before ep2 in EPS list.");
			log.error("ep1: "+ret);
			log.error("ep2: "+next);
			ret = null;
		}
		
		return ret;
	}
	/**
	 * find out an FvPair whose Rargname matches name and whose Var's label matches label
	 *  
	 * @param Rargname, such as "ARG0"
	 * @param label label of Var, such as "e2"
	 * @return a matching FvPair
	 */
	public FvPair getFvpairByRargnameAndIndex (String name, String label) {
		
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if (f.getRargname().equals(name) && f.getVar().getLabel().equals(label)) {
					return f;
				}
			}
		}
		
		return null;
		
	}
	
	public void addEPtoEPS (ElementaryPredication ep) {
		if (ep!=null) this.eps.add(ep);
	}
	
	/**
	 * Add a simple HCONS to hcons, such as "h1 qeq h2"
	 * @param hreln "qeq"
	 * @param hi_vid "1"
	 * @param hi_sort "h"
	 * @param lo_vid "2"
	 * @param lo_sort "h"
	 */
	public void addToHCONSsimple (String hreln, String hi_vid, String hi_sort,
			String lo_vid, String lo_sort) {
		this.hcons.add(new HCONS(hreln, hi_vid, hi_sort, lo_vid, lo_sort));
	}
	
	/**
	 * When FSC is input to cheap, NEs are labeled as NAMED_UNK_REL,
	 * which generates the following error in LKB generation:
	 * Warning: invalid predicates: |named_unk_rel("Washington DC")|
	 * Changing named_unk_rel to NAMED_REL solves this (hopefully).
	 */
	public void changeFromUnkToNamed () {
		for (ElementaryPredication ep:this.eps) {
			if (ep.getPred()!=null && ep.getPred().equalsIgnoreCase("NAMED_UNK_REL")) {
				ep.setPred("NAMED_REL");
			}
		}
	}
	
	/**
	 * get a string containing an MRX
	 * @return a one-line string with an <mrs> element
	 */
	public String toMRXstring() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		toXML(os);
		String mrx = os.toString();
		return mrx;
	}
	
	public void toXML(OutputStream os) {
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		// LKB doesn't support properly indented xml files. thus set indentation off.
		of.setIndenting(false);
		//of.setIndent(1);
		//of.setDoctype(null, "mrs.dtd");
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
	
	/**
	 * This method builds cross references for an MRS representation.
	 * After reading/parsing an MRX, such arguments (such as "x9) are 
	 * referenced individually. This method make them refer to the same one.
	 */
	public void buildCoref() {
		HashMap<String, Var> varM = new HashMap<String, Var>();
		String label;
		Var v;
		for (ElementaryPredication ep: eps) {
			for (FvPair p:ep.getFvpair()) {
				v = p.getVar();
				// only deal with argument
				if (v!=null && v.getSort().equals("x")) {
					label = v.getLabel();
					if (varM.get(label) == null) {
						varM.put(label, v);
					} else {
						p.setVar(varM.get(label));
					}
				}
			}
		}
	}
	
	/**
	 * This method parses a MRS document in XML, then calls {@link #buildCoref}. 
	 * 
	 * @param file an MRS XML fil
	 */
	public void parse(File file) {
		this.parser.parse(file);
		buildCoref();
	}
	
	/**
	 * This method parses a MRS document in a string, then calls {@link #buildCoref}. 
	 * 
	 * @param str a string containing an MRS structure
	 */
	public void parseString(String str) {
		this.parser.parseString(str);
		buildCoref();
	}
	
	/**
	 * 
	 * Use RegEx to match all <mrs/> in a multiline string
	 * 
	 * @param multiline cheap output spreading multilines 
	 * @return an array list containing all <mrs/> elements
	 */
	public static ArrayList<String> getMrxStringsFromCheap (String multiline) {
		ArrayList<String> list = new ArrayList<String>();
		
		Pattern p = Pattern.compile("<mrs>(.*?)<\\/mrs>", 
				Pattern.MULTILINE|Pattern.DOTALL);
		Matcher m = p.matcher(multiline);
		while (m.find()) {
			String mrs = m.group();
			list.add(mrs);
		}
		
		if (list.size()==0) {
			log.error("Cheap output:");
			log.error("No parsed MRS from Cheap:\n"+multiline);
		}
		return list;
	}
	
	public static void main(String args[]) 
	throws Exception {
		MRS m = new MRS();
		for(String file:args) {
			File f = new File(file);
			m.parse(f);
			System.out.println("done");
			m.toXML(System.out);
			System.out.println(m);
		}
	}
}