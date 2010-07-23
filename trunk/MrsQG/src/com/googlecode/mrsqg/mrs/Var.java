package com.googlecode.mrsqg.mrs;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
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

	private static Logger log = Logger.getLogger(Var.class);

	protected String vid = null;
	protected String sort = null;
	// label = sort+vid
	protected String label = null;
//	;;; <!ELEMENT extrapair (path,value)>
//	;;; <!ELEMENT path (#PCDATA)>
//	;;; <!ELEMENT value (#PCDATA)>
	// use LinkedHashMap to get insertion order of keys
	protected LinkedHashMap<String, String> extrapair = null;
	private String path;

	public String getVid() {return vid;}
	public String getSort() {return sort;}
	public String getLabel() {return label;}
	public String getPath() {return path;}
	public LinkedHashMap<String, String> getExtrapair() {return extrapair;}
	public void setSort(String s) {sort = s; label=sort+vid;}
	public void setVid(String s) {vid = s; label=sort+vid;}
	public void setLabel(String value) {
		this.vid = value.substring(1);
		this.sort = value.substring(0, 1);
		this.label = value;
	}

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
		// values and keys of this LinkedHashMap are of class String.
		// so shallow copy equals deep copy in this case.
		this.extrapair = (LinkedHashMap<String, String>)old.getExtrapair().clone();
	}

	public Var(String vid, String sort) {
		this.vid = vid;
		this.sort = sort;
		this.label = sort+vid;
		this.extrapair = new LinkedHashMap<String, String>();
	}

	public Var(String value) {
		this.vid = value.substring(1);
		this.sort = value.substring(0, 1);
		this.label = sort+vid;
		this.extrapair = new LinkedHashMap<String, String>();
	}

	/**
	 * Construct a complex Var such as "e13 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]"
	 * @param value "e13"
	 * @param extraPairs {"SF", "PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"}
	 */
	public Var(String value, String[] extraPairs) {
		this.vid = value.substring(1);
		this.sort = value.substring(0, 1);
		this.label = sort+vid;
		this.extrapair = new LinkedHashMap<String, String>();
		int size = extraPairs.length/2;
		for (int i=0; i<size; i++) {
			extrapair.put(extraPairs[2*i], extraPairs[2*i+1]);
		}
	}

	public Var(Attributes atts) {
		vid = atts.getValue("vid");
		sort = atts.getValue("sort");
		label = sort+vid;
		extrapair = new LinkedHashMap<String, String>();
	}

//	@Override public boolean equals (Object obj) {
//		Var v = (Var) obj;
//		boolean ret = false;
//		if (this.vid.equals(v.getVid()) && this.sort.equals(v.getVid())
//				&& this.label.equals(v.getLabel())
//				&& this.extrapair.equals(v.getExtrapair())) {
//			ret = true;
//		}
//
//		return ret;
//	}

	public void newExtraPair () {

	}

	public void updatePath (String path) {
		this.path = path;
	}

	public void updateValue (String value) {
		extrapair.put(path, value);
	}

	/**
	 * Keep some extrapair in var and remove all others.
	 *
	 * @param extra extrapair to be kept, such as {"NUM", "PERS"}
	 */
	public void keepExtrapair(String[] extra) {
		ArrayList<String> extraL = new ArrayList<String>();
		for (int i=0; i<extra.length; i++) {
			extraL.add(extra[i]);
		}
		String[] ik = extrapair.keySet().toArray(new String[extrapair.keySet().size()]);

		for (String k:ik) {
			if (!extraL.contains(k))
				extrapair.remove(k);
		}
	}

	/**
	 * Set the value of extrapair. E.g. path="SF", value="QUES"
	 * set the value of the "SF" extrapair to "QUES".
	 *
	 * @param path path of this extrapair
	 * @param value value of this extrapair
	 */
	public void setExtrapairValue(String path, String value) {
		for (String key:extrapair.keySet()) {
			if (key.equals(path)) {
				extrapair.put(path, value);
				break;
			}
		}
	}

	/**
	 * Add an extra pair to exiting ones, such as add "IND: +" to  [ x PERS: 3 NUM: SG ]
	 * @param extraFeature
	 * @param extraValue
	 */
	public void addExtrapair(String extraFeature, String extraValue) {
		if (this.extrapair != null)
			this.extrapair.put(extraFeature, extraValue);

		return;
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
					if (value == null || path == null) continue;
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
			log.error(e);
		}
	}

}
