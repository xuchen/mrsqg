package com.googlecode.mrsqg.mrs;

import org.apache.log4j.Logger;
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
	private static Logger log = Logger.getLogger(HCONS.class);
	private String rel = null;
//	private String hi = null;
//	private String lo = null;
	private Var hiVar = null;

	// could be either Var or label
	private Var loVar = null;
	/** <code>loLabel</code> is rarely used in <!ELEMENT lo (label|var)>. use loVar instead. */
	private String loLabel = null;

	public String getRel() {return rel;}
	public String getHi() {return hiVar.getLabel();}
	public String getLo() {return loVar.getLabel();}
	public Var getHiVar() {return hiVar;}
	public Var getLoVar() {return loVar;}
	public String getLoLabelRare() {return loLabel;}
	public void setRel(String s) {rel=s;}
	public void setHi(String s) {if (hiVar!=null) hiVar.setLabel(s);}
	public void setLo(String s) {if (loVar!=null) loVar.setLabel(s);}
	public void setHiVar(Var v) {hiVar=v;}
	public void setLoVar(Var v) {loVar=v;}
	public void setLoLabelRare (String s) {loLabel=s;}

	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		// h5 qeq h7
		res.append(getHi()+" "+rel+" "+getLo());
		return res.toString();
	}
	/**
	* Copy constructor.
	*/
	public HCONS(HCONS old) {
		if (old == null) return;
		this.rel = old.getRel();
		this.hiVar = new Var(old.getHiVar());
		this.loVar = new Var(old.getLoVar());
		this.loLabel = old.getLoLabelRare();
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
		this.hiVar = new Var(hi_vid, hi_sort);
		this.loVar = new Var(lo_vid, lo_sort);
	}

	/**
	 * A simple constructor, such as "h1 qeq h2"
	 * @param hreln "qeq"
	 * @param hi "h1"
	 * @param lo "h2"
	 */
	public HCONS(String hreln, String hi, String lo) {
		this.rel = hreln;
		this.hiVar = new Var(hi);
		this.loVar = new Var(lo);
	}

	public boolean checkValid() {
		if (rel != null && hiVar != null && (loVar != null || loLabel != null) ) {
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
			hiVar.serializeXML(hd);
			hd.endElement("", "", "hi");
			// <lo>
			atts.clear();
			hd.startElement("", "", "lo", atts);
			if (loVar != null) {
				loVar.serializeXML(hd);
			} else if (loLabel != null) {
				// <label>
				atts.clear();
				atts.addAttribute("", "", "vid", "CDATA", loLabel);
				hd.startElement("", "", "label", atts);
				hd.endElement("", "", "label");
			} else {

				System.err.println("Error, <lo> must have either <lo> or <label>");
			}
			hd.endElement("", "", "lo");

			hd.endElement("", "", "hcons");
		} catch (SAXException e) {
			log.error(e);
		}

	}
}

