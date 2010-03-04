package com.googlecode.mrsqg.mrs;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class HCONS {
//	;;; <!ELEMENT hcons (hi, lo)>
//	;;; <!ATTLIST hcons 
//	;;;          hreln (qeq|lheq|outscopes) #REQUIRED >
//	;;;
//	;;; <!ELEMENT hi (var)>
//	;;; <!ELEMENT lo (label|var)>

	
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
	public void setRel(String s) {rel=s;}
	public void setHi(String s) {hi=s;}
	public void setLo(String s) {lo=s;}
	public void setHiVar(Var v) {hi_var=v;}
	public void setLoVar(Var v) {lo_var=v;}
	public void setLoLabel(String s) {lo_label=s;}
	
	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		// h5 qeq h7
		res.append(hi+" "+rel+" "+lo);
		return res.toString();
	}
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
	
	/**
	 * A simple constructor, such as "h1 qeq h2"
	 * @param hreln "qeq"
	 * @param hi_vid "1"
	 * @param hi_sort "h"
	 * @param lo_vid "2"
	 * @param lo_sort "h"
	 */
	public HCONS(String hreln, String hi_vid, String hi_sort,
			String lo_vid, String lo_sort) {
		this.rel = hreln;
		this.hi = hi_sort+hi_vid;
		this.lo = lo_sort+lo_vid;
		this.hi_var = new Var(hi_vid, hi_sort);
		this.lo_var = new Var(lo_vid, lo_sort);
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

