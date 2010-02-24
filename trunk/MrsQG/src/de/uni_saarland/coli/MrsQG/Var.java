package de.uni_saarland.coli.MrsQG;

import java.util.ArrayList;

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
	public ArrayList<ExtraPair> extrapair = null;
	public ExtraPair currentExtraPair = null;
	
	public Var(Attributes atts) {
		vid = atts.getValue("vid");
		sort = atts.getValue("sort");
		label = sort+vid;
		extrapair = new ArrayList<ExtraPair>();
	}
	
	public void newExtraPair () {
		currentExtraPair = new ExtraPair();
		extrapair.add(currentExtraPair);
	}
	
	public void updatePath (String path) {
		currentExtraPair.path = path;
	}
	
	public void updateValue (String value) {
		currentExtraPair.value = value;
	}
	
//	;;; <!ELEMENT extrapair (path,value)>
//	;;; <!ELEMENT path (#PCDATA)>
//	;;; <!ELEMENT value (#PCDATA)>
	private class ExtraPair {
		public String path = null;
		public String value = null;
	}
}
