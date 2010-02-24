package de.uni_saarland.coli.MrsQG;

import java.util.ArrayList;
import java.util.TreeMap;

import org.xml.sax.Attributes;

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
	


}
