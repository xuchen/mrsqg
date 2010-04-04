package com.googlecode.mrsqg.mrs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * An MRS representation class.<p>
 * Naming convention: <br/>
 * In an example EP: <br/>
 * [ <br/>
 *   _poet_n_1_rel<4:8> <br/>
 *   LBL: h10 <br/>
 *   ARG0: x6 [ x PERS: 3 NUM: SG IND: + ] <br/>
 * ] <br/>
 * <b>type(EP) label</b>: _poet_n_1_rel <br/>
 * <b>feature</b>: LBL/ARG0 <br/>
 * <b>value</b>: h10/x6 <br/>
 * <b>FvPair (feature/value pair)</b>: LBL: h10 <br/>
 * <b>extra type (Var)</b>: [ x PERS: 3 NUM: SG IND: + ], h10 (simplified value)<br/>
 * <b>extra feature</b>: PERS/NUM/IND <br/>
 * <b>extra value</b>: 3/SG/+<br/>
 *
 * @author Xuchen Yao
 * 
 *
 */
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
	private String sent_type = "PROP";
	
	private ArrayList <ElementaryPredication> eps;
	private ArrayList<HCONS> hcons;
	private MrsParser parser = new MrsParser();
	
	public String getLTOP() {return ltop;}
	public String getLabelVid() {return label_vid;}
	public String getIndex() {return index;}
	public String getIndexVid() {return index_vid;} 
	public String getSentType() {return sent_type;}
	public ArrayList <ElementaryPredication> getEps() {return eps;}
	public ArrayList<HCONS> getHcons() {return hcons;}
	public void setSentType (String sentForce) {sent_type = sentForce;};
	public void setIndex (String index) {
		if (!index.startsWith("e")) {
			log.warn("the main event of this MRS doesn't start with an e: "+index+"!");
		}
		this.index = index;
		// index usually looks like "e2".
		this.index_vid = index.substring(1);
	}

	
	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("\n");
		res.append("SentType: "+sent_type+"\n");
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
		this.sent_type = old.getSentType();
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
					h.setLoLabelRare(atts.getValue("vid"));
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
					h.setHiVar(new Var(atts));
				} else if (parent.equals("lo")) {
					String sort = atts.getValue("sort");
					// get the last one in the list
					HCONS h = hcons.get(hcons.size()-1);
					// should be sth. like "h11"
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
	 * Return a list of EP with a label value <code>label</code>.
	 * for instance, return all EPs with a label value "h3".
	 * @param label the label value of the EP, such as "h3"
	 * @return an ArrayList of EP with the matching label
	 */
	public ArrayList<ElementaryPredication> getEPbyLabelValue (String label) {
		ArrayList<ElementaryPredication> retEP = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:eps) {
			if (ep.getLabel().equals(label)) {
				retEP.add(ep);
				break;
			}
		}
		
		return retEP.size() == 0? null: retEP;
	}
	
	/**
	 * Return a list of EP elements with a type name <code>name</code>.
	 * @param name a type name, such as "APPOS_REL"
	 * @return an ArrayList of EP with a matching type name, or null if no matching.
	 */
	public ArrayList<ElementaryPredication> getEPbyTypeName (String name) {
		ArrayList<ElementaryPredication> retEP = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:eps) {
			if (ep.getTypeName().equals(name)) {
				retEP.add(ep);
				break;
			}
		}
		return retEP.size() == 0? null: retEP;
	}
	
	/**
	 * get a list of the feature labels of all EPs
	 * @return an ArrayList containing all the labels of EPS
	 */
	public ArrayList<String> getEPSfeatList () {
		ArrayList<String> list = new ArrayList<String>();
		for(ElementaryPredication ep:eps) {
			list.add(ep.getLabel());
		}
		
		return list;
	}
	
	/**
	 * Get a list of the values of all EPs.<p>
	 * In an EP like: <br/>
	 * [ _like_v_1_rel<5:10><br/>
  	 * LBL: h8<br/>
  	 * ARG0: e9<br/>
  	 * ARG1: x6<br/>
     * ARG2: x10<br/>
	 * ]<br/>
	 * h8 is label. e9, x6, x10 are handles
	 * @return an ArrayList containing all the values of EPS
	 */
	public ArrayList<String> getEPSvalueList () {
		ArrayList<String> list = new ArrayList<String>();
		for(ElementaryPredication ep:eps) {
			for (FvPair fp:ep.getFvpair()) {
				list.add(fp.getValue());
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
	 * Find out a list of extra type whose extra feature/value match <code>feat</code> and <code>value</code>.
	 *  
	 * @param feat the name of extra feature, such as "ARG0"
	 * @param value the name of extra value, such as "e2"
	 * @return a list of matching FvPair
	 */
	public ArrayList<FvPair> getExtraTypeByFeatAndValue (String feat, String value) {
		
		ArrayList<FvPair> list = new ArrayList<FvPair>();
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if (f.getRargname().equals(feat) && f.getVar().getLabel().equals(value)) {
					list.add(f);
				}
			}
		}
		return list.size() == 0 ? null : list;
	}
	
	/**
	 * Find out a list of extra type whose extra value match <code>value</code>.
	 *  
	 * @param value the name of extra value, such as "e2"
	 * @return a list of matching FvPair
	 */
	public ArrayList<FvPair> getExtraTypeByValue (String value) {
		
		ArrayList<FvPair> list = new ArrayList<FvPair>();
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if (f.getVar().getLabel().equals(value)) {
					list.add(f);
				}
			}
		}
		return list.size() == 0 ? null : list;
	}
	
	/**
	 * find out an EP whose feature/value match <code>feat</code> and <code>value</code>
	 *  
	 * @param feat feature name, such as "ARG0"
	 * @param value value name, such as "e2"
	 * @return an ArrayList of matching EP
	 */
	public ArrayList<ElementaryPredication> getEPbyFeatAndValue (String feat, String value) {
		if (value==null) return null;
		ArrayList<ElementaryPredication> list = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:this.eps) {
			for (FvPair f: ep.getFvpair()) {
				if (f.getRargname().equals(feat) && f.getVar().getLabel().equals(value)) {
					list.add(ep);
				}
			}
		}
		return list;
	}
	
	public void addEPtoEPS (ElementaryPredication ep) {
		if (ep!=null) this.eps.add(ep);
	}
	
	/**
	 * Get the tense of this MRS. in a malformed MRS, it's possible that there's no tense found,
	 * in this case return "PRES"
	 * @return a string of tense
	 */
	public String getTense () {
		try {
			return (getExtraTypeByValue(getIndex()).get(0)).getVar().getExtrapair().get("TENSE");
		} catch (NullPointerException e) {
			// 
			return "PRES";
		}
	}
	
	/**
	 * Set the tense of this MRS
	 * @param tense
	 */
	public void setTense (String tense) {
		ArrayList<FvPair> list = getExtraTypeByValue(getIndex());
		for (FvPair p:list) 
			p.getVar().getExtrapair().put("TENSE", tense);
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
	
	public void addToHCONSsimple (String hreln, String hi, String lo) {
		this.hcons.add(new HCONS(hreln, hi, lo));
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
	 * Give an EP ArrayList of size 2, determine which is the HiEP, and return the 
	 * index (0 or 1).
	 * @param eps an EP ArrayList of size 2
	 * @param mrs the MRS in which <code>eps</code> comes from
	 * @return an index 0 or 1, or -1 if error
	 */
	public static int determineHiEPindex (ArrayList<ElementaryPredication> eps, MRS mrs) {
		if (eps.size() != 2) {
			log.error("EPS size should be exactly 2!\n" + eps);
		}
		
		int hiIdx;
		String hi, lo, rstr;
		ElementaryPredication hiEP, loEP;
		
		hiEP = loEP = eps.get(0);
		hi = lo = eps.get(0).getLabel();
		rstr = eps.get(1).getValueByFeature("RSTR");
		if (rstr == null) {
			hiIdx = 0;
			loEP = eps.get(1);
			lo = loEP.getLabel();
			hi = hiEP.getValueByFeature("RSTR");
		} else {
			hiEP = eps.get(1);
			hi = rstr;
			try {
				assert lo == rstr;
			} catch (AssertionError e) {
				log.error("In eps:\n"+eps+"\none should refer" +
				"the other in RSTR field");
				return -1;
			}
			hiIdx = 1;
		}
		// check whether hi and lo match HCONS
		boolean match = false;
		for (HCONS h: mrs.getHcons()) {
			if (h.getHi().equals(hi)) {
				try {
					assert h.getLo().equals(lo);
					assert h.getRel().equals("qeq");
					match = true;
					break;
				} catch (AssertionError e) {
					log.error("hi "+hi+" and lo "+lo+" don't match" +
							" with HCONS: "+h);
					continue;
				}
			}
		}
		if (!match) {
			log.error("hi "+hi+" and lo "+lo+" don't match" +
					" with HCONS: "+mrs.getHcons());
			return -1;
		}
		
		return hiIdx;
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
	 * Get a list of FvPair which has value matching <code>value</code>.
	 * @param value a matching value, such as "x2".
	 * @return an ArrayList of FvPair
	 */
	public ArrayList<FvPair> getFvPairByValue (String value) {
		ArrayList<FvPair> list = new ArrayList<FvPair>();
		for (ElementaryPredication ep:this.eps) {
			for (FvPair p:ep.getFvpair()) {
				if (p.getValue()!=null && p.getValue().equals(value)) {
					list.add(p);
					break;
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
	 * extract a new MRS from mrs, containing only EPs that are indirectly associated with label.
	 * currently, the label should only be a predicate's label. for instance, an EP looks like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * then all EPs with x6 and x10 as ARG* (indirectly) are extracted. Those EPs make a new MRS.
	 * @param label the label value of the predicate, such as "h8"
	 * @param mrs the original mrs to be extracted from 
	 * @return a new MRS with only EPs concerning label 
	 */
	public static MRS extractByLabelValue (String label, MRS mrs) {
		MRS extracted = new MRS(mrs);
		// targetEP is the one with a label as the label in the parameter
		
		ElementaryPredication targetEP = null;
		for (ElementaryPredication ep:extracted.getEPbyLabelValue(label)) {
			if (ep.getArg0().equals(mrs.getIndex())) {
				// the target EP is the main predicate of this MRS
				targetEP = ep;
			}
		}
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
		extracted.cleanHCONS();
		
		return extracted;
	}
	
	/**
	 * Extract a new MRS from mrs, containing only EPs that are directly associated with label.
	 * This method is used when the label isn't a predicate's label. for instance, an EP looks like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * then all EPs with x6 and x10 as ARG0 (directly) are extracted. Those EPs make a new MRS.
	 * @param targetEP an EP to find references for
	 * @param mrs the original mrs to be extracted from 
	 * @return a new MRS with only EPs concerning <code>targetEP</code> 
	 */
	public static MRS extractByEPandArg0 (ElementaryPredication targetEP, MRS mrs) {
		
		if (targetEP == null) {
			log.error("Can't find the EP " + targetEP +" in MRS:\n" + mrs);
			return null;
		}
		
		MRS extracted = new MRS(mrs);
		
		HashSet<String> argSet = targetEP.getAllARGvalue();
		if (argSet.size() <= 1) {
			log.warn("the EP "+targetEP+" contains less than 2 ARG." +
					" Decomposition will probably fail.");
		}
		
		extracted.markDeletionByEPref(targetEP);

		extracted.removeEPbyFlag();
		// clean up HCONS list
		extracted.cleanHCONS();
		
		return extracted;
	}
	
	/**
	 * Clean up the HCONS list. Any HCONS pairs, such as "h1 qeq h2", whose
	 * hiLabel and loLabel can't be both found the the EPS type labels are removed.
	 */
	public void cleanHCONS () {
		ArrayList<String> labelList = this.getEPSfeatList();
		ArrayList<String> handleList = this.getEPSvalueList();
		ArrayList<HCONS> hcopy = new ArrayList<HCONS>(this.getHcons()); 
		for (HCONS h:hcopy) {
			if (!handleList.contains(h.getHi()) || !labelList.contains(h.getLo())) {
				if (!this.removeHCONS(h)) {
					log.error("Error: HCONS "+h+" can't be removed from MRS:\n" + this);
				}
			}
		}
	}
	
	/**
	 * This method retrieves the labels
	 * of all EPs which are referred by the ARG0 values of ep.
	 * @param ep An EP which has ARG* entries 
	 * @return a HashSet of labels referred by the ARG0 of this ep
	 */
	public HashSet<String> getAllReferredLabelByEP (ElementaryPredication ep) {
		HashSet<String> labelSet = new HashSet<String>();
		
		labelSet.add(ep.getLabel());
		HashSet<String> argList = ep.getAllARGvalue();
		
		for (ElementaryPredication e:getEps()) {
			if (e==ep) continue;
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
	 * Mark deletion of one EP by judging its ARG0 doesn't refer to 
	 * any ARG* values of <code>ep</code>
	 * @param ep An EP which has ARG* entries
	 */
	public void markDeletionByEPref (ElementaryPredication ep) {

		HashSet<String> argList = ep.getAllARGvalue();
		
		for (ElementaryPredication e:getEps()) {
			if (e==ep) continue;
			if (!argList.contains(e.getArg0())) {
				e.setFlag(true);
			}
		}
	}
	
	/**
	 * remove <code>ep</code> from the EPS list
	 * @param ep the ep to be removed
	 * @return success status
	 */
	public boolean removeEP (ElementaryPredication ep) {
		
		if (eps.remove(ep)==false) {
			log.error("Can't remove ep:\n"+ep+"\nfrom EPS list:\n"+eps);
			return false;
		}
		return true;
	}
	
	/**
	 * remove all EPs whose flag is set to true from the EPS list
	 * @return a boolean success status
	 */
	public boolean removeEPbyFlag () {
		ArrayList<ElementaryPredication> removedList = new ArrayList<ElementaryPredication>();
		for (ElementaryPredication ep:this.eps) {
			if (ep.getFlag() == true) {
				removedList.add(ep);
			}
		}
		if(this.eps.removeAll(removedList)) {
			return true;
		} else {
			log.error("removing EP by flag failed!");
			return false;
		}
		// the following code contains a bug and thus is depreciated.
//		ArrayList<ElementaryPredication> concurrentList = new ArrayList<ElementaryPredication> (this.eps);
//		
//		for (ElementaryPredication ep:concurrentList) {
//			if (ep.getFlag() == true) {
//				this.eps.remove(concurrentList.indexOf(ep));
//			}
//		}
	}
	
	/**
	 * remove a <code>list</code> of EP from the EPS list
	 * @param list an ArrayList of EP
	 * @return success status
	 */
	public boolean removeEPlist (ArrayList<ElementaryPredication> list) {
		boolean ret = false;
		for (ElementaryPredication ep:list) {
			ret = eps.remove(ep);
			if (!ret) {
				log.error("Can't remove ep:\n"+ep+"\nfrom EPS list:\n"+eps);
				break;
			}
		}
		
		return ret;
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
	 * Generate a list of the index for unused labels.
	 * 
	 * @param num the number of unused labels to return
	 * @return an ArrayList containing <code>num</code> members, such as "7", "8", "9", ...
	 */
	public ArrayList<String> generateUnusedLabel (int num) {
		HashSet<Integer> valueSet = new HashSet<Integer>();
		ArrayList<String> list = new ArrayList<String>();
		
		// LKB takes index globally, so we count everything under "h/x/e/p..."
		// http://lists.delph-in.net/archive/developers/2010/001402.html
		for (ElementaryPredication e:getEps()) {
			for (String value:e.getAllValue()) {
				valueSet.add(Integer.parseInt(value.substring(1)));
			}
			valueSet.add(Integer.parseInt(e.getLabel().substring(1)));
		}
		
		Integer[] valueArray = (Integer[]) valueSet.toArray(new Integer[]{});
		Arrays.sort(valueArray);
		for (int i=valueArray[valueArray.length-1]+1; num>0; i++, num--) {
			list.add(String.valueOf(i));
		}
		
		return list;				
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