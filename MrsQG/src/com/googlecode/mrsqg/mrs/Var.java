package com.googlecode.mrsqg.mrs;

import java.util.ArrayList;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/*
 * Data structure used to hold <var> element in an MRS xml file.
 * <var> is used by both <fvpair> and <mrs> thus it can't be
 * declared private in ElementaryPredication 
 */
public class Var {
//	;;; <!ELEMENT var (extrapair*)>
//	;;; <!ATTLIST var
//	;;;          vid  CDATA #REQUIRED 
//	;;;          sort (x|e|h|u|l) #IMPLIED >	
	
	/*
	 * !!! WARNING !!!
	 * Any new field added to this class must also be added to the copy constructor. 
	 */
	
	private String vid = null;
	private String sort = null;
	// label = sort+vid
	private String label = null;
//	;;; <!ELEMENT extrapair (path,value)>
//	;;; <!ELEMENT path (#PCDATA)>
//	;;; <!ELEMENT value (#PCDATA)>
	private TreeMap<String, String> extrapair = null;
	private String path;
	
	public String getVid() {return vid;}
	public String getSort() {return sort;}
	public String getLabel() {return label;}
	public String getPath() {return path;}
	public TreeMap<String, String> getExtrapair() {return extrapair;}
	public void setSort(String s) {sort = s; label=sort+vid;}
	public void setVid(String s) {vid = s; label=sort+vid;}
	
	@Override public String toString() {
		// x6 [ x PERS: 3 NUM: SG IND: + ]
		StringBuilder res = new StringBuilder();
		res.append(label);
		if (extrapair.size()!=0) {
			String value;
			res.append(" [ "+sort+" ");
			for (String path: extrapair.keySet()) {
				value = extrapair.get(path);
				res.append(path+": "+value+" ");
			}
			res.append("]");
		}
		
		return res.toString();
	}

	/**
	* Copy constructor.
	*/
	public Var(Var old) {
		if (old == null) return;
		this.vid = old.getVid();
		this.sort = old.getSort();
		this.label = old.getLabel();
		this.path = old.getPath();
		// values and keys of this TreeMap are of class String.
		// so shallow copy equals deep copy in this case.
		this.extrapair = (TreeMap<String, String>)old.getExtrapair().clone();
	}
	
	public Var(Attributes atts) {
		vid = atts.getValue("vid");
		sort = atts.getValue("sort");
		label = sort+vid;
		extrapair = new TreeMap<String, String>();
	}
	
	public void newExtraPair () {

	}
	
	public void updatePath (String path) {
		this.path = path;
	}
	
	public void updateValue (String value) {
		extrapair.put(path, value);
	}
	
	public void serializeXML (ContentHandler hd) {
		
		// <var vid='5' sort='x'>
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", "vid", "CDATA", vid);
		if (sort != null) {
			atts.addAttribute("", "", "sort", "CDATA", sort);
		}
		try {
			hd.startElement("", "", "var", atts);
			// extrapair
			if (!extrapair.isEmpty()) {
				//<extrapair><path>PERS</path><value>3</value></extrapair>
				atts.clear();
				for (String path: extrapair.keySet()) {
					String value = extrapair.get(path);
					hd.startElement("", "", "extrapair", atts);
					hd.startElement("", "", "path", atts);
					hd.characters(path.toCharArray(),0, path.length());
					hd.endElement("", "", "path");
					hd.startElement("", "", "value", atts);
					hd.characters(value.toCharArray(),0, value.length());
					hd.endElement("", "", "value");
					hd.endElement("", "", "extrapair");
				}
			}

			// warning:
			// the original format is: <var vid='4' sort='h'></var>
			// but this generates: <var vid='4' sort='h'/>
			hd.endElement("", "", "var");
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

}
