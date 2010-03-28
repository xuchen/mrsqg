package com.googlecode.mrsqg.mrs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	public void setIndex (String index) {
		this.index = index;
		// index usually looks like "e2".
		this.index_vid = index.substring(1);
	}

	
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
	 * return the ElementaryPredication element with a label.
	 * for instance, return the EP with a label "x2"
	 * @param label the label of the EP, such as "x2"
	 * @return an EP with the matching label
	 */
	public ElementaryPredication getEPbyLabel (String label) {
		ElementaryPredication retEP = null;
		for (ElementaryPredication ep:eps) {
			if (ep.getLabel().equals(label)) {
				retEP = ep;
				break;
			}
		}
		
		return retEP;
	}
	
	/**
	 * get a list of the labels of all EPs
	 * @return an ArrayList containing all the labels of EPS
	 */
	public ArrayList<String> getEPSlabelList () {
		ArrayList<String> list = new ArrayList<String>();
		for(ElementaryPredication ep:eps) {
			list.add(ep.getLabel());
		}
		
		return list;
	}
	
	/**
	 * get a list of the handles of all EPs
	 * In an EP like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * h8 is label. e9, x6, x10 are handles
	 * @return an ArrayList containing all the handles of EPS
	 */
	public ArrayList<String> getEPShandleList () {
		ArrayList<String> list = new ArrayList<String>();
		for(ElementaryPredication ep:eps) {
			for (FvPair fp:ep.getFvpair()) {
				list.add(fp.getHandle());
			}
		}
		
		return list;
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
	
	/**
	 * find out an EP whose Rargname matches name and whose Var's label matches label
	 *  
	 * @param Rargname, such as "ARG0"
	 * @param label label of Var, such as "e2"
	 * @return a matching EP
	 */
	public ElementaryPredication getEPbyRargnameAndIndex (String name, String label) {
		if (label==null) return null;
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if (f.getRargname().equals(name) && f.getVar().getLabel().equals(label)) {
					return ep;
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
	 * Given a hiLabel, check the corresponding loLabel in the list.
	 * For instance, the list contains a "h1 qeq h2" relation, then
	 * given a hiLabel "h1", the function returns the loLabel "h2"
	 * @param hiLabel a hiLabel
	 * @param list a list possibly containing the loLabel
	 * @return a corresponding loLabel in the list, or null if not found
	 */
	public static String getLoLabelFromHconsList (String hiLabel, ArrayList<HCONS> list) {
		String loLabel = null;
		if (hiLabel != null) {
			for (HCONS h:list) {
				if(h.getHi().equals(hiLabel)) {
					loLabel = h.getLo();
					break;
				}
			}
		}
		return loLabel;
	}
	
	/**
	 * Given a loLabel, check the corresponding HiLabel in the list.
	 * For instance, the list contains a "h1 qeq h2" relation, then
	 * given a loLabel "h2", the function returns the hiLabel "h1"
	 * @param loLabel a loLabel
	 * @param list a list possibly containing the hiLabel
	 * @return a corresponding hiLabel in the list, or null if not found
	 */
	public static String getHiLabelFromHconsList (String loLabel, ArrayList<HCONS> list) {
		String hiLabel = null;
		if (loLabel != null) {
			for (HCONS h:list) {
				if(h.getLo().equals(loLabel)) {
					hiLabel = h.getHi();
					break;
				}
			}
		}
		return hiLabel;
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
	 * Get all the extra pairs in this MRS. Extra pairs are sth. like: [TENSE: PRES] 
	 * encoded in XML: <extrapair><path>TENSE</path><value>PRES</value></extrapair>
	 * @return an ArrayList of all extra pairs
	 */
	public ArrayList<LinkedHashMap<String, String>> getExtrapair () {
		ArrayList<LinkedHashMap<String, String>> list = new ArrayList<LinkedHashMap<String, String>>();
		Var v;
		LinkedHashMap<String, String> p;
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if((v=f.getVar())!= null) {
					if ((p=v.getExtrapair())!=null) {
						list.add(p);
					}
				}
			}
		}

		return list;
	}
	
	/**
	 * Set the sentence force of all events variables to "QUES". Theoretically only
	 * the events of predicates should be set, but practically all of them are set.
	 */
	public void setAllSF2QUES () {
		for (LinkedHashMap<String, String> p:getExtrapair()) {
			if (p.get("SF")!=null) {
				p.put("SF", "QUES");
			}
		}
	}
	
	/**
	 * extract a new MRS from mrs, containing only EPs that are associated with label.
	 * currently, the label should only of a predicate's label. for instance, an EP looks like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * then all EPs with x6 and x10 as ARG0 are extracted. Those EPs make a new MRS.
	 * @param label the label of the predicate
	 * @param mrs the original mrs to be extracted from 
	 * @return a new MRS with only EPs concerning label 
	 */
	public static MRS extractByLabel (String label, MRS mrs) {
		MRS extracted = new MRS(mrs);
		// targetEP is the one with a label as the label in the parameter 
		ElementaryPredication targetEP = extracted.getEPbyLabel(label);
		// targetEPS is a list of all EPs that have connections with targetEP
 
		if (targetEP == null) {
			log.error("Can't find the EP with a label" + label +" in MRS:\n" + mrs);
			return null;
		}
		
		HashSet<String> argSet = targetEP.getAllARGvalue();
		HashSet<String> referredLabelSet = extracted.getAllReferredLabelByEP(targetEP);
		if (argSet.size() <= 1) {
			log.warn("the EP "+targetEP+" contains less than 2 ARG." +
					" Decomposition will probably fail.");
		}
		
		// suppose targetEP is the main predicate of the sentence,
		// then the main event would be ARG0 of targetEP.
		String event = targetEP.getArg0();
		if (event.startsWith("e")) {
			extracted.setIndex(event);
		} else {
			log.error("ARG0 of EP isn't an event: "+targetEP);
		}
		
		// Remove all EPs whose ARG0 is not associated with ARG* of targetEP
		// TODO: this is only the simplest case. A good algorithm should do it
		// recursively: other relevant EPs might be attached to EPs which are 
		// not targetEP (Currently also consider the labels of relevant 
		// EPs who have ARG0 in the argSet of targetEP -- so it's not that simple case).
		ArrayList<ElementaryPredication> copy = new ArrayList<ElementaryPredication>(extracted.getEps());
		for (ElementaryPredication ep:copy) {
			if (!argSet.contains(ep.getArg0()) && !referredLabelSet.contains(ep.getLabel())) {
				if (!extracted.removeEP(ep)) {
					log.error("Error: EP " +ep+ " can't be removed from MRS:\n" + extracted);
				}
			}
		}
		
		// clean up HCONS list
		ArrayList<String> labelList = extracted.getEPSlabelList();
		ArrayList<String> handleList = extracted.getEPShandleList();
		ArrayList<HCONS> hcopy = new ArrayList<HCONS>(extracted.getHcons()); 
		for (HCONS h:hcopy) {
			if (!handleList.contains(h.getHi()) || !labelList.contains(h.getLo())) {
				if (!extracted.removeHCONS(h)) {
					log.error("Error: HCONS "+h+" can't be removed from MRS:\n" + extracted);
				}
			}
		}
		
		return extracted;
	}
	
	/**
	 * An EP refers to other EPs by the ARG* values. This method retrieves the labels
	 * of all EPs which are referred by the ARG* values of ep.
	 * @param ep An EP which has ARG* entries 
	 * @return a HashSet of labels referred by the ARG* of this ep
	 */
	public HashSet<String> getAllReferredLabelByEP (ElementaryPredication ep) {
		HashSet<String> labelSet = new HashSet<String>();
		
		labelSet.add(ep.getLabel());
		HashSet<String> argList = ep.getAllARGvalue();
		
		for (ElementaryPredication e:getEps()) {
			for (String label:argList) {
				if (e.getArg0().equals(label)) {
					labelSet.add(e.getLabel());
					break;
				}
			}
		}
		
		return labelSet;
	}
	/**
	 * remove ep from the EPS list
	 * @param ep the ep to be removed
	 * @return success status
	 */
	public boolean removeEP (ElementaryPredication ep) {
		return eps.remove(ep);
	}
	
	/**
	 * remove h from the hcons list
	 * @param h the HCONS to be removed
	 * @return success status
	 */
	public boolean removeHCONS (HCONS h) {
		return hcons.remove(h);
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