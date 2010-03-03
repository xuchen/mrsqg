package com.googlecode.mrsqg.mrs;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


public class FvPair {
//	<!ELEMENT fvpair (rargname, (var|constant))>
//	<!ELEMENT rargname (#PCDATA)>
//	<!ELEMENT constant (#PCDATA)>
	
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
	public void setRargname(String s) {rargname = s;}
	public void setConstant(String s) {constant = s;}
	public void setVar(Var v) {var = v;}
	
	@Override public String toString() {
		// RSTR: h5
		// ARG0: x6 [ x PERS: 3 NUM: SG IND: + ]
		StringBuilder res = new StringBuilder();
		res.append(rargname+": ");
		if (var!=null) res.append(var);
		// CARG: "Al Gore"
		if (constant!=null) res.append("\""+constant+"\"");
		
		return res.toString();
	}
	
	/**
	* Copy constructor.
	*/
	public FvPair(FvPair old) {
		if (old == null) return;
		this.rargname = old.getRargname();
		this.constant = old.getConstant();
		if (old.getVar()!=null)
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
