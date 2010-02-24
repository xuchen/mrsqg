package de.uni_saarland.coli.MrsQG;

import java.util.ArrayList;
import java.util.TreeMap;

import org.xml.sax.Attributes;


public class ElementaryPredication {
//	<!ELEMENT ep ((pred|realpred), label, fvpair*)>
//	<!ATTLIST ep
//	          cfrom CDATA #IMPLIED
//	          cto   CDATA #IMPLIED 
//	          surface   CDATA #IMPLIED
//	      base      CDATA #IMPLIED >

	public int cfrom = -1;
	public int cto = -1;
	public String surface = null;
	public String base = null;
	public String pred = null;
	public String spred = null;
	public String label = null;
	public ArrayList<FvPair> fvpair = null;
	public FvPair currentFvPair = null;

	// I hate to do it this way, but it's not that easy to 
	// read LISP XML output in Java...
	private class FvPair {
//		<!ELEMENT fvpair (rargname, (var|constant))>
//		<!ELEMENT rargname (#PCDATA)>
//		<!ELEMENT constant (#PCDATA)>
		public String rargname = null;
		public String constant = null;
		public Var var = null;
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
			label = "h"+atts.getValue("vid");
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
}
