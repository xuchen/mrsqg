package com.googlecode.mrsqg.mrs;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.googlecode.mrsqg.util.StringUtils;


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

	private static Logger log = Logger.getLogger(ElementaryPredication.class);

	private int cfrom = -1;
	private int cto = -1;
	private String surface = null;
	private String base = null;
	/** "upper case" relations*/
	private String pred = null;
	/** "lower case" relations */
	private String spred = null;
	private String label = null;
	private String label_vid = null;
	private ArrayList<FvPair> fvpair = null;
	private FvPair currentFvPair = null;
	/** A set of EPs which govern the current EP by ARG*. In the theory of a
	 * A -> B relation, A is called a head and B is its dependent. We call
	 * A a governor here since "head" is ambiguous if used alone. */
	private HashSet <ElementaryPredication> governorsByArg = null;
	/** A set of EPs which govern the current EP by relations other
	 * than ARG*, such as RSTR */
	private HashSet<ElementaryPredication> governorsByNonArg = null;

	private HashSet <ElementaryPredication> dependentsByArg = null;
	private HashSet<ElementaryPredication> dependentsByNonArg = null;

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
		this.flag = old.getFlag();
		this.fvpair = new ArrayList<FvPair>();
		for(FvPair p:old.getFvpair()) {
			this.fvpair.add(new FvPair(p));
		}
		governorsByArg = new HashSet<ElementaryPredication>();
		governorsByNonArg = new HashSet<ElementaryPredication>();
		dependentsByArg = new HashSet<ElementaryPredication>();
		dependentsByNonArg = new HashSet<ElementaryPredication>();
	}


	public ElementaryPredication() {
		fvpair = new ArrayList<FvPair>();
		governorsByArg = new HashSet<ElementaryPredication>();
		governorsByNonArg = new HashSet<ElementaryPredication>();
		dependentsByArg = new HashSet<ElementaryPredication>();
		dependentsByNonArg = new HashSet<ElementaryPredication>();
	}

	public ElementaryPredication(String typeName, String label) {
		this();
		if (StringUtils.containsUppercase(typeName))
			this.pred = typeName;
		else
			this.spred = typeName;
		this.cfrom = 0;
		this.cto = 0;
		this.setLabel(label);
	}

	/**
	 * The use of flag purely serves engineering purposes. In decomposition, some EPs are
	 * needed to be removed after a copy construction. But it's not easy to trace these EPs
	 * by any sort of equals() methods. So a flag is set to mark these to-be-removed EPs.
	 */
	private boolean flag = false;

	public int getCfrom() {return cfrom;}
	public int getCto() {return cto;}
	public String getSurface() {return surface;}
	public String getBase() {return base;}
	public String getPred() {return pred;}
	public String getSpred() {return spred;}
	public String getLabel() {return label;}
	public String getLabelVid() {return label_vid;}
	public ArrayList<FvPair> getFvpair() {return fvpair;}
	public String getTypeName() {if (pred!=null) return pred; else return spred;};
	public boolean getFlag () {return flag;}
	public void setFlag (boolean f) {this.flag = f;}

	public void setPred(String s) {pred=s;}
	public void setSpred(String s) {spred=s;}
	public void setLabelVid(String s) {label_vid=s;label="h"+s;}
	public void setLabel(String s) {label=s; label_vid=s.substring(1);}

	public HashSet<ElementaryPredication> getGovernorsByArg () { return governorsByArg;}
	public HashSet<ElementaryPredication> getGovernorsByNonArg () { return governorsByNonArg;}
	public HashSet<ElementaryPredication> getDependentsByArg () { return dependentsByArg;}
	public HashSet<ElementaryPredication> getDependentsByNonArg () { return dependentsByNonArg;}
	/**
	 * Add an EP to the set of governors which refer to the current EP by ARG*.
	 * @param ep An EP
	 */
	public void addGovernorByArg (ElementaryPredication ep) {
		governorsByArg.add(ep);
	}

	/**
	 * Add an EP to the set of governors which refer to the current EP by relations
	 * other than ARG*.
	 * @param ep An EP
	 */
	public void addGovernorByNonArg (ElementaryPredication ep) {
		governorsByNonArg.add(ep);
	}

	/**
	 * Add an EP to the set of dependents referred by the current EP by ARG*.
	 * @param ep An EP
	 */
	public void addDependentByArg (ElementaryPredication ep) {
		dependentsByArg.add(ep);
	}

	/**
	 * Add an EP to the set of dependents referred by the current EP by relations
	 * other than ARG*.
	 * @param ep An EP
	 */
	public void addDependentByNonArg (ElementaryPredication ep) {
		dependentsByNonArg.add(ep);
	}

	/**
	 * Return all EPs that are the governor or dependent of this EP
	 * @return a HashSet of EPs
	 */
	public HashSet<ElementaryPredication> getAllConnections() {
		HashSet<ElementaryPredication> connections = new HashSet<ElementaryPredication>();
		connections.addAll(governorsByArg);
		connections.addAll(governorsByNonArg);
		connections.addAll(dependentsByArg);
		connections.addAll(dependentsByNonArg);
		return connections;
	}

	public void setTypeName(String typeName) {
		if (StringUtils.containsUppercase(typeName)) {
			this.pred = typeName;
			this.spred = null;
		} else {
			this.spred = typeName;
			this.pred = null;
		}
	}
	/**
	 * return all "ARG*" values in this EP.
	 * for instance, an EP looks like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * then it returns a list containing "e9", "x6" and "x10"
	 *
	 * @return a HashSet containing all "ARG*" values
	 */
	public HashSet<String> getAllARGvalue() {

		HashSet<String> set = new HashSet<String>();
		for (FvPair fp:fvpair) {
			if (fp.getFeature().startsWith("ARG")) {
				set.add(fp.getVar().getLabel());
			}
		}

		return set;
	}

	/**
	 * return all "ARG*" except ARG0 values in this EP.
	 * for instance, an EP looks like:
	 * [ _like_v_1_rel<5:10>
  	 * LBL: h8
  	 * ARG0: e9
  	 * ARG1: x6
     * ARG2: x10
	 * ]
	 * then it returns a list containing "e9", "x6" and "x10"
	 *
	 * @return a HashSet containing all "ARG*" values
	 */
	public HashSet<String> getAllARGvalueExceptARG0() {

		HashSet<String> set = new HashSet<String>();
		for (FvPair fp:fvpair) {
			if (fp.getFeature().startsWith("ARG") && !fp.getFeature().equals("ARG")) {
				set.add(fp.getVar().getLabel());
			}
		}

		return set;
	}

	/**
	 * Get all values in an EP, such as "x9", "e2", etc.
	 * @return a HashSet containing all "ARG*" values
	 */
	public HashSet<String> getAllValue() {

		HashSet<String> set = new HashSet<String>();
		for (FvPair fp:fvpair) {
			if (fp.getVar() != null)
				set.add(fp.getVar().getLabel());
		}

		return set;
	}


	/**
	 * Get all values and label in an EP, such as "x9", "e2", etc.
	 * @return a HashSet containing all "ARG*" values
	 */
	public HashSet<String> getAllValueAndLabel() {

		HashSet<String> set = new HashSet<String>();
		for (FvPair fp:fvpair) {
			if (fp.getVar() != null)
				set.add(fp.getVar().getLabel());
		}
		set.add(this.getLabel());

		return set;
	}

	/*
	public ArrayList<String> getAllARGvalue() {
		// TreeMap guarantees that the map will be in ascending key order
		// so the returned values are sorted by their keys
		TreeMap<String, String> map = new TreeMap<String, String>();
		ArrayList<String> list = new ArrayList<String>();
		for (FvPair fp:fvpair) {
			if (fp.getRargname().startsWith("ARG")) {
				map.put(fp.getRargname(), fp.getVar().getLabel());
			}
		}
		for (String v:(String[])map.values().toArray(new String[0])) {
			list.add(v);
		}
		return list;
	}
	*/

	/**
	 * return the ARG0 value of this EP, if any
	 * @return the ARG0 value, such as "e2", or null if none
	 */
	public String getArg0() {
		return getValueByFeature("ARG0");
	}

	/**
	 * Whether this EP is an EP for verbs, whose type name matches "_v_"
	 * and ARG0 is an event (with SF:PROP).
	 * @return a boolean value
	 */
	public boolean isVerbEP() {
		return this.getTypeName().contains("_v_") && this.getArg0().startsWith("e");
	}

	/**
	 * Return the value of a feature.
	 *
	 * @param s can be "ARG0", "RSTR", "BODY", "ARG1", "ARG2"...
	 * @return a label, such as "x3", or null if not found
	 */
	public String getValueByFeature (String s) {
		String label = null;
		s = s.toUpperCase();
		for (FvPair p:fvpair) {
			if (p.getRargname().equals(s)) {
				label = p.getValue();
				break;
			}
		}
		return label;
	}

	/**
	 * Return the extra type (Var) of a feature.
	 * @param feat can be "ARG0", "RSTR", "BODY", "ARG1", "ARG2"...
	 * @return a corresponding Var
	 */
	public Var getValueVarByFeature (String feat) {
		Var v = null;

		for (FvPair p:fvpair) {
			if (p.getRargname().equalsIgnoreCase(feat)) {
				v = p.getVar();
				break;
			}
		}
		return v;
	}

	/**
	 * Delete a FvPair with a specific label.
	 *
	 * @param s can be "ARG0", "RSTR", "BODY", "ARG1", "ARG2"...
	 */
	public void delFvpair(String s) {
		s = s.toUpperCase();
		for (FvPair p:fvpair) {
			if (p.getRargname().equals(s)) {
				fvpair.remove(p);
				break;
			}
		}
	}

	/**
	 * add a simple FvPair (such as "RSTR: h9") to this EP
	 *
	 * @param rargname "RSTR"
	 * @param vid "9"
	 * @param sort "h"
	 */
	public void addSimpleFvpair(String rargname, String vid, String sort) {
		FvPair p = new FvPair(rargname, vid, sort);
		this.fvpair.add(p);
	}

	/**
	 * add a simple FvPair (such as "RSTR: h9") to this EP
	 *
	 * @param feature "RSTR"
	 * @param value "h9"
	 */
	public void addSimpleFvpair(String feature, String value) {
		FvPair p = new FvPair(feature, value);
		this.fvpair.add(p);
	}

	/**
	 * add a complex FvPair to this EP.
	 * For instance, "ARG0: e13 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]"
	 *
	 * @param feature "ARG0"
	 * @param value "e13"
	 * @param extraPairs {"SF", "PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"}
	 */
	public void addFvpair(String feature, String value, String[] extraPairs) {
		FvPair p = new FvPair(feature, value, extraPairs);
		this.fvpair.add(p);
	}

	/**
	 * add a complex FvPair to this EP.
	 * @param feature a feature string, such as "ARG0".
	 * @param value a Var value.
	 */
	public void addFvpair(String feature, Var value) {
		FvPair p = new FvPair(feature, value);
		this.fvpair.add(p);
	}

	/**
	 * Set the value of a feature. For instance, set the value of
	 * <code>feature</code> "ARG0" to "x3" (a Var).
	 * @param feature feature's name
	 * @param value a Var
	 */
	public void setFvpairByFeatAndValue (String feature, Var value) {
		for (FvPair p:fvpair) {
			if (p.getFeature().equals(feature)) {
				p.setVar(value);
				break;
			}
		}
	}

	/**
	 * Keep some extrapair in fvpair and remove all others.
	 *
	 * @param fv can be "ARG0", "RSTR", "BODY", "ARG1", "ARG2"...
	 * @param extra extrapair to be kept, such as {"NUM", "PERS"}
	 */
	public void keepExtrapairInFvpair(String fv, String[] extra) {
		fv = fv.toUpperCase();

		for (FvPair p:fvpair) {
			if (p.getRargname().equals(fv)) {
				p.getVar().keepExtrapair(extra);
				break;
			}
		}
	}

	public void keepFvpair(String[] pairs) {
		ArrayList<String> list = StringUtils.arrayToArrayList(pairs);
		ArrayList<FvPair> fvlist = new ArrayList<FvPair>();

		for (FvPair p:fvpair) {
			if (!list.contains(p.getFeature())) {
				fvlist.add(p);
			}
		}

		if (fvlist.size() != 0 && !fvpair.removeAll(fvlist)) {
			log.error("Removing fvlist from fvpair failed!");
			log.error("fvlist: " + fvlist);
			log.error("fvpair: " + fvpair);
		}
	}



	/**
	 * return the Var list in fvpair.
	 */
	public ArrayList<Var> getVarList() {
		ArrayList<Var> varL = new ArrayList<Var>();
		Var v;
		for (FvPair p: fvpair) {
			v = p.getVar();
			if (v != null) varL.add(v);
		}
		return varL;
	}

	/**
	 * Add a value <code>shift</code> to the range.
	 * @param shift
	 */
	public void shiftRange (int shift) {
		cfrom += shift;
		cto += shift;
	}

	@Override public String toString() {
//		<!ELEMENT ep ((pred|realpred), label, fvpair*)>
//		<!ATTLIST ep
//		          cfrom CDATA #IMPLIED
//		          cto   CDATA #IMPLIED
//		          surface   CDATA #IMPLIED
//		      base      CDATA #IMPLIED >
		StringBuilder res = new StringBuilder();
		/*
		    [ proper_q_rel<0:7>
            LBL: h3
            ARG0: x6 [ x PERS: 3 NUM: SG IND: + ]
            RSTR: h5
            BODY: h4 ]
		 */
		if (pred!= null) res.append("[ "+pred);
		if (spred!= null) res.append("[ "+spred);
		if (cfrom!=-1 && cto!=-1) {
			res.append("<"+Integer.toString(cfrom)+":"+Integer.toString(cto)+">");
		}
		if (surface != null) log.debug("complete the code in toString()!");
		if (base != null) log.debug("complete the code in toString()!");
		res.append("\n");
		res.append("  LBL: "+label+"\n");
		for (FvPair p:fvpair) {
			res.append("  "+p+"\n");
		}
		res.append("]");
		return res.toString();
	}


//	@Override public boolean equals (Object obj) {
//		ElementaryPredication ep = (ElementaryPredication)obj;
//		boolean ret = true;
//
//		return ret;
//	}

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
			currentFvPair.setVar(new Var(atts));
		} else if (qName.equals("extrapair")) {
			currentFvPair.getVar().newExtraPair();
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
			currentFvPair.setRargname(str);
		} else if (qName.equals("constant")) {
			currentFvPair.setConstant(str);
		} else if (qName.equals("path")) {
			currentFvPair.getVar().updatePath(str);
		} else if (qName.equals("value")) {
			currentFvPair.getVar().updateValue(str);
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
