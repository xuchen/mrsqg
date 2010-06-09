package com.googlecode.mrsqg;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * This class transforms the MRS of a declarative sentence
 * into that of a interrogative sentence.
 *
 * @author Xuchen Yao
 * @version 2010-03-02
 */
public class MrsTransformer {

	private static Logger log = Logger.getLogger(MrsTransformer.class);

	private Preprocessor pre;

	/**
	 * MRS for the original sentence
	 */
	private MRS ori_mrs;

	/**
	 * a list of MRS for the generated questions.
	 * Each question is represented by one MRS
	 */
	private ArrayList<MRS> gen_mrs;

	public MrsTransformer (File file, Preprocessor p) {
		this.ori_mrs = new MRS(file);
		this.pre = p;
		this.gen_mrs = new ArrayList<MRS>();
	}

	public MrsTransformer (String mrx, Preprocessor p) {
		this.ori_mrs = new MRS(mrx);
		this.pre = p;
		this.gen_mrs = new ArrayList<MRS>();
	}

	public MrsTransformer (MRS mrs, Preprocessor p) {
		this.ori_mrs = new MRS(mrs);
		this.pre = p;
		this.gen_mrs = new ArrayList<MRS>();
	}

	public ArrayList<MRS> transform (boolean print) {
		ArrayList<MRS> trMrsList;
		Term[] terms = pre.getTerms()[0];

		// generate yes/no question
		// change SF to "QUES"
		// e2
		MRS q_mrs = transformYNques();

		this.gen_mrs.add(q_mrs);

		trMrsList = transformWHques(terms);
		if (trMrsList != null)
			this.gen_mrs.addAll(trMrsList);

		trMrsList = transformHOWques(terms);
		if (trMrsList != null)
			this.gen_mrs.addAll(trMrsList);

		if (print) {
			for (MRS m:this.gen_mrs) {
				log.info(m.getSentType()+" question MRX:");
				log.info(m.toMRXstring());
				log.info(m);
			}
		}

		return this.gen_mrs;
	}

	/**
	 * Transform from a declarative to Y/N interrogative.
	 *
	 * @return an interrogative MRS
	 */
	public MRS transformYNques () {
		// generate yes/no question
		// change SF to "QUES"
		// e2
		MRS q_mrs = new MRS(this.ori_mrs);
//		String index = q_mrs.getIndex();
//		FvPair v = q_mrs.getExtraTypeByFeatAndValue("ARG0", index);
		// It's possible that v==null in some malformed MRS that
		// no EP refers to the main event by ARG0
//		if (v==null) {
//			log.error("FvPair ARG0: "+index+" not found! " +
//					"can't set SF to QUES! from the following MRS:\n"+q_mrs);
//		}
//		if (null != v.getVar()) v.getVar().setExtrapairValue("SF", "QUES");
		q_mrs.setSF2QUES();

		q_mrs.changeFromUnkToNamed();
		q_mrs.setSentType("Y/N");
		q_mrs.setSF2QUES();

		return q_mrs;
	}

	/**
	 * Transform from a declarative to WH (who/which/what/where/when) interrogative.
	 * @param terms all the terms in this MRS.
	 * @return an ArrayList of generated interrogative MRS, or null if none.
	 */
	public ArrayList<MRS> transformWHques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
		ArrayList<ElementaryPredication> eps;

		for (Term term:terms) {
			for (String neType:term.getNeTypes()) {
				//neType = Arrays.toString(term.getNeTypes());
				if (neType.length()==0) {
					log.error("NE types shouldn't be none: "+term);
					continue;
				}

				//if (neType.contains("NEperson")||neType.contains("NElocation")||neType.contains("NEdate"))
				q_mrs = new MRS(this.ori_mrs);
				eps = q_mrs.getEPS(term.getCfrom(), term.getCto());
				String hi, lo;
				ElementaryPredication hiEP, loEP;
				if (eps.size() == 1) {
					loEP = eps.get(0);
					lo = loEP.getLabel();
					// hiEP should be found through a qeq relation
					hi = MRS.getHiLabelFromHconsList(lo, q_mrs.getHcons());
					if (hi == null) continue;
					ArrayList<ElementaryPredication> rstr = q_mrs.getEPbyFeatAndValue("RSTR", hi);
					if (rstr==null) continue;
					hiEP = rstr.get(0);
					if (hi==null||hiEP==null) {
						/*
						 * It seems in a well-formed MRS, eps.size() is always 2.
						 * when eps.size() is 1, just use another MRS input.
						 */
						log.warn("the size of eps is 1: "+eps);
						log.warn("Can't find a qeq relation for "+lo+" in "+q_mrs.getHcons());
						continue;
					}
				} else if (eps.size() == 2) {
					// one is hi, the other is lo in a qeq relation
					int hiIdx = MRS.determineHiEPindex (eps, q_mrs);
					if (hiIdx == -1) continue;
					hiEP = eps.get(hiIdx);
					loEP = eps.get(1-hiIdx);
				} else if (eps == null || eps.size() == 0) {
					continue;
				} else {
					log.warn("the size of eps isn't 1 or 2: "+eps);
					continue;
				}

				// change hiEP to which_q_rel
				hiEP.setTypeName("WHICH_Q_REL");

				// change loEP to person_rel
				if (neType.equals("NEperson")||neType.equals("NEfirstName")) {
					loEP.setTypeName("PERSON_REL");
					q_mrs.setSentType("WHO");

				} else if (neType.equals("NElocation")) {
					loEP.setTypeName("PLACE_N_REL");
					loEP.getValueVarByFeature("ARG0").setExtrapairValue("NUM", "SG");
					q_mrs.setSentType("WHERE");
//					loEP.setTypeName("THING_REL");
//					q_mrs.setSentType("WHAT");
				} else if (neType.equals("NEdate")||neType.equals("NEtime")) {
					loEP.setTypeName("TIME_N_REL");
					q_mrs.setSentType("WHEN");

				} else {
					loEP.setTypeName("THING_REL");
					// only keep "ARG0" as the feature
					loEP.keepFvpair(new String[]{"ARG0"});
					// also "ARG0" should be the same with hiEP
					loEP.setFvpairByFeatAndValue("ARG0", hiEP.getValueVarByFeature("ARG0"));
					q_mrs.setSentType("WHAT");
				}
				loEP.delFvpair("CARG");
				String[] extra = {"NUM", "PERS"};
				loEP.keepExtrapairInFvpair("ARG0", extra);

				if (neType.equals("NElocation")) {
					// NElocation generates two types of questions: where and which place
					// this scope of code generates the "which place" question
					MRS placeMrs = new MRS(q_mrs);
					// set hiEP to _WHICH_Q_REL and loEP to _place_n_of_rel
					placeMrs.getEPbyParallelIndex(q_mrs, hiEP).setTypeName("_WHICH_Q_REL");
					ElementaryPredication placeEP = placeMrs.getEPbyParallelIndex(q_mrs, loEP);
					placeEP.setTypeName("_place_n_of_rel");
					placeEP.addSimpleFvpair("ARG1", "i"+placeMrs.generateUnusedLabel(1).get(0));
					placeEP.getValueVarByFeature("ARG0").addExtrapair("IND", "+");
					placeMrs.setSF2QUES();
					placeMrs.changeFromUnkToNamed();
					placeMrs.setSentType("WHICH");
					outList.add(placeMrs);
				}


				if (neType.equals("NElocation") || neType.equals("NEdate"))
				{
					ElementaryPredication ppEP = q_mrs.getEPbefore(term.getCfrom(), term.getCto());
					String pp = this.pre.getPrepositionBeforeTerm(term, 0);
					// change the preposition (if any) before the term
					if (pp!=null && ppEP != null && ppEP.getPred()!=null &&
							ppEP.getPred().substring(0, pp.length()+1).toLowerCase().contains(pp.toLowerCase())) {
						// the Pred of an "in" preposition EP is something like: _in_p_
						// so the first 3 chars _in must contain "in"
						ppEP.setTypeName("LOC_NONSP_REL");
						if (neType.equals("NElocation")) {
							loEP.setTypeName("PLACE_N_REL");
							loEP.getValueVarByFeature("ARG0").setExtrapairValue("NUM", "SG");
							// only keep "ARG0" as the feature
							loEP.keepFvpair(new String[]{"ARG0"});
							// also "ARG0" should be the same with hiEP
							loEP.setFvpairByFeatAndValue("ARG0", hiEP.getValueVarByFeature("ARG0"));
							q_mrs.setSentType("WHERE");
						}
					}

				}


				// change SF to "QUES"
				q_mrs.setSF2QUES();
				q_mrs.changeFromUnkToNamed();
				outList.add(q_mrs);
			}
		}
		return outList.size() == 0 ? null : outList;
	}

	/**
	 * Transform from a declarative to how many/much interrogative.
	 * Note: currently LKB doesn't generate from this function, for unknown reason.
	 * @param terms all the terms in this MRS.
	 * @return an ArrayList of generated interrogative MRS, or null if none.
	 */
	public ArrayList<MRS> transformHOWques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
		ArrayList<ElementaryPredication> eps;

		for (Term term:terms) {
			for (String neType:term.getNeTypes()) {
				//neType = Arrays.toString(term.getNeTypes());
				if (neType.length()==0) {
					log.error("NE types shouldn't be none: "+term);
				}

				if (neType.contains("NEnumber")||neType.contains("NEhour")||neType.contains("NEpercentage")) {
					q_mrs = new MRS(this.ori_mrs);
					eps = q_mrs.getEPS(term.getCfrom(), term.getCto());

					// there should be two: one is UDEF_Q_REL, the other is card_rel
					if (eps.size() != 2) {
						log.error("the size of eps isn't 2: "+eps);
						continue;
					}

					ElementaryPredication hiEP, loEP;

					// one is hi, the other is lo in a qeq relation
					int hiIdx = MRS.determineHiEPindex (eps, q_mrs);
					if (hiIdx == -1) continue;
					hiEP = eps.get(hiIdx);
					loEP = eps.get(1-hiIdx);

					// loEP should be "CARD_REL"
					if (!loEP.getTypeName().equals("CARD_REL")) {
						log.warn("The EP's type name is not CARD_REL in a how many/much question:\n"+loEP);
					}
					loEP.delFvpair("CARG");
					loEP.setTypeName("MUCH-MANY_A_REL");

					// construct a new qeq relation with:
					// WHICH_Q_REL qeq ABSTR_DEG_REL
					/*
			          [ abstr_deg_rel<0:1>
			            LBL: h7
			            ARG0: x8 ]
			          [ which_q_rel<0:1>
			            LBL: h9
			            ARG0: x8
			            RSTR: h11
			            BODY: h10 ]
					 */
					ArrayList<String> labelStore = q_mrs.generateUnusedLabel(6);
					ElementaryPredication whichEP = new ElementaryPredication("WHICH_Q_REL", "h"+labelStore.get(0));
					String arg0 = "x"+labelStore.get(4);
					whichEP.addSimpleFvpair("ARG0", arg0);
					whichEP.addSimpleFvpair("RSTR", "h"+labelStore.get(1));
					whichEP.addSimpleFvpair("BODY", "h"+labelStore.get(2));
					ElementaryPredication abstrEP = new ElementaryPredication("ABSTR_DEG_REL", "h"+labelStore.get(3));
					abstrEP.addSimpleFvpair("ARG0", arg0);
					q_mrs.addEPtoEPS(whichEP);
					q_mrs.addEPtoEPS(abstrEP);
					q_mrs.addToHCONSsimple("qeq", "h"+labelStore.get(1), "h"+labelStore.get(3));

					// construct a new EP "MEASURE_REL" with the same label of loEP
					// and takes WHICH_Q_REL as ARG2 (ARG0 and ARG1 are all events)
					// and takes loEP's ARG0 as ARG1
					/*
			          [ measure_rel<0:1>
			            LBL: h12
			            ARG0: e13 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]
			            ARG1: e14 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]
			            ARG2: x8 ]
					 */
					ElementaryPredication measureEP = new ElementaryPredication("MEASURE_REL", loEP.getLabel());
					String[] extraPairs = {"SF", "PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"};
					measureEP.addFvpair("ARG0", "e"+labelStore.get(5), extraPairs);
					measureEP.addFvpair("ARG1", loEP.getArg0(), extraPairs);
					measureEP.addSimpleFvpair("ARG2", arg0);
					q_mrs.addEPtoEPS(measureEP);

					// change SF to "QUES"
					q_mrs.setSF2QUES();
					q_mrs.setSentType("HOW MANY/MUCH");
					q_mrs.changeFromUnkToNamed();
					// build cross references?
					q_mrs.buildCoref();
					outList.add(q_mrs);
				}
			}
		}
		return outList.size() == 0 ? null : outList;
	}


}
