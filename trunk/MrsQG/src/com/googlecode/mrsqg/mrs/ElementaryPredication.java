package com.googlecode.mrsqg.mrs;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;



public class ElementaryPredication {
//	<!ELEMENT ep ((pred|realpred), label, fvpair*)>
//	<!ATTLIST ep
//	          cfrom CDATA #IMPLIED
//	          cto   CDATA #IMPLIED 
//	          surface   CDATA #IMPLIED
//	      base      CDATA #IMPLIED >
	
	/*
	 * !!! WARNING !!!
	 * Any new field added to this class must also be added to the copy constructor. 
	 */
	
	private int cfrom = -1;
	private int cto = -1;
	private String surface = null;
	private String base = null;
	private String pred = null;
	private String spred = null;
	private String label = null;
	private String label_vid = null;
	private ArrayList<FvPair> fvpair = null;
	private FvPair currentFvPair = null;
	
	public int getCfrom() {return cfrom;}
	public int getCto() {return cto;}
	public String getSurface() {return surface;}
	public String getBase() {return base;}
	public String getPred() {return pred;}
	public String getSpred() {return spred;}
	public String getLabel() {return label;}
	public String getLabelVid() {return label_vid;}
	public ArrayList<FvPair> getFvpair() {return fvpair;}
	
	/**
	* Copy constructor.
	*/
	public ElementaryPredication(ElementaryPredication old) {
		if (old == null) return;
		this.cfrom = old.getCfrom();
		this.cto = old.getCto();
		this.surface = old.getSurface();
		this.base= old.getBase();
		this.pred = old.getPred();
		this.spred = old.getSpred();
		this.label = old.getLabel();
		this.label_vid = old.getLabelVid();
		this.fvpair = new ArrayList<FvPair>();
		for(FvPair p:old.getFvpair()) {
			this.fvpair.add(new FvPair(p));
		}
	}

	// I hate to do it this way, but it's not that intuitive to 
	// read LISP XML output in Java...
	private class FvPair {
//		<!ELEMENT fvpair (rargname, (var|constant))>
//		<!ELEMENT rargname (#PCDATA)>
//		<!ELEMENT constant (#PCDATA)>
		
		/*
		 * !!! WARNING !!!
		 * Any new field added to this class must also be added to the copy constructor. 
		 */
		
		private String rargname = null;
		private String constant = null;
		private Var var = null;
		
		public String getRargname() {return rargname;}
		public String getConstant() {return constant;}
		public Var getVar() {return var;}
		
		/**
		* Copy constructor.
		*/
		public FvPair(FvPair old) {
			if (old == null) return;
			this.rargname = old.getRargname();
			this.constant = old.getConstant();
			this.var = new Var(old.getVar());
		}
		
		public FvPair() {
		}
		
		public void serializeXML (ContentHandler hd) {
			AttributesImpl atts = new AttributesImpl();
			try {
				// <rargname>ARG0</rargname>
				atts.clear();
				hd.startElement("", "", "rargname", atts);
				hd.characters(rargname.toCharArray(), 0, rargname.length());
				hd.endElement("", "", "rargname");
				
				if (var!=null) {
					var.serializeXML(hd);
				} else if (constant != null) {
					// <constant>John</constant>
					atts.clear();
					hd.startElement("", "", "constant", atts);
					hd.characters(constant.toCharArray(), 0, constant.length());
					hd.endElement("", "", "constant");
				}
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}
	}

	public ElementaryPredication() {
		fvpair = new ArrayList<FvPair>();
	}
	
	public void processStartElement (String qName, Attributes atts) {
//		;;; <!ELEMENT ep ((pred|realpred), label, fvpair*)>
//		;;; <!ATTLIST ep
//		;;;          cfrom CDATA #IMPLIED
//		;;;          cto   CDATA #IMPLIED 
//		;;;          surface   CDATA #IMPLIED
//		;;;      base      CDATA #IMPLIED >

		if (qName.equals("ep")) {
			cfrom = Integer.parseInt(atts.getValue("cfrom"));
			cto = Integer.parseInt(atts.getValue("cto"));
			if (atts.getValue("surface") != null) {
				System.err.println("surface atts in <ep> element. " +
						"complete your code!");
			}
			if (atts.getValue("base") != null) {
				System.err.println("base atts in <ep> element. " +
						"complete your code!");
			}
		} else if (qName.equals("label")) {
			label_vid = atts.getValue("vid");
			label = "h"+label_vid;
		} else if (qName.equals("fvpair")) {
			currentFvPair = new FvPair();
			fvpair.add(currentFvPair);
		} else if (qName.equals("var")) {
			currentFvPair.var = new Var(atts);
		} else if (qName.equals("extrapair")) {
			currentFvPair.var.newExtraPair();
		}
	}
	
	public void processEndElement (String qName, String str) {
		if (qName.equals("pred")) {
			pred = str;
		} else if (qName.equals("spred")) {
			spred = str;
		} else if (qName.equals("realpred")) {
			// no such situation in sample files, need to complete
			// this part once met
			System.err.println("<realpred>: Manually check the code and complete it!");
		} else if (qName.equals("rargname")) {
			currentFvPair.rargname = str;
		} else if (qName.equals("constant")) {
			currentFvPair.constant = str;
		} else if (qName.equals("path")) {
			currentFvPair.var.updatePath(str);			
		} else if (qName.equals("value")) {
			currentFvPair.var.updateValue(str);			
		}
	}
	
	public void serializeXML (ContentHandler hd) {
//		<!ELEMENT ep ((pred|realpred), label, fvpair*)>
//		<!ATTLIST ep
//		          cfrom CDATA #IMPLIED
//		          cto   CDATA #IMPLIED 
//		          surface   CDATA #IMPLIED
//		      base      CDATA #IMPLIED >
		AttributesImpl atts = new AttributesImpl();
		try {
			// <ep cfrom='0' cto='3'>
			atts.addAttribute("", "", "cfrom", "CDATA", Integer.toString(cfrom));
			atts.addAttribute("", "", "cto", "CDATA", Integer.toString(cto));
			if (base!=null)
				atts.addAttribute("", "", "surface", "CDATA", surface);
			if (base!=null)
				atts.addAttribute("", "", "base", "CDATA", base);
			hd.startElement("", "", "ep", atts);
			
			if (pred!=null) {
				//<pred>PROPER_Q_REL</pred>
				atts.clear();
				hd.startElement("", "", "pred", atts);
				hd.characters(pred.toCharArray(), 0, pred.length());
				hd.endElement("", "", "pred");
			} else if (spred!=null) {
				//<spred>_like_v_1_rel</pred>
				atts.clear();
				hd.startElement("", "", "spred", atts);
				hd.characters(spred.toCharArray(), 0, spred.length());
				hd.endElement("", "", "spred");
			}
			
			//<label vid='3'/>
			atts.clear();
			atts.addAttribute("", "", "vid", "CDATA", label_vid);
			hd.startElement("", "", "label", atts);
			hd.endElement("", "", "label");
			
			//<fvpair>
			for (FvPair p : fvpair) {
				p.serializeXML(hd);
			}
			hd.endElement("", "", "ep");
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
	}
}
