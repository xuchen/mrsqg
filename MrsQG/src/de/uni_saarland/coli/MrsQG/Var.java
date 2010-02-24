package de.uni_saarland.coli.MrsQG;

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
	public String vid = null;
	public String sort = null;
	// label = sort+vid
	public String label = null;
//	;;; <!ELEMENT extrapair (path,value)>
//	;;; <!ELEMENT path (#PCDATA)>
//	;;; <!ELEMENT value (#PCDATA)>
	public TreeMap<String, String> extrapair = null;
	private String path;

	
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
