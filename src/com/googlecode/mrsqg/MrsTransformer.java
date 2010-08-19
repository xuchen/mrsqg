package com.googlecode.mrsqg;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.EP;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * This class transforms the MRS of a declarative sentence
 * into that of a interrogative sentence.
 *
 * @author Xuchen Yao
 * @version 2010-03-02
 *
 * @deprecated Use {@link com.googlecode.mrsqg.MrsTransformer2} instead
 */
public class MrsTransformer {

	private static Logger log = Logger.getLogger(MrsTransformer.class);

	protected Preprocessor pre;

	/**
	 * MRS for the original sentence
	 */
	protected MRS ori_mrs;

	/**
	 * a list of MRS for the generated questions.
	 * Each question is represented by one MRS
	 */
	protected ArrayList<MRS> gen_mrs;

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

		if (ori_mrs.getSentType().equals("WHY")) {
			/*
			 * The WhyDecomposer also generates a "WHY" question,
			 * thus we don't do a transform on a "WHY" question.
			 */
			this.gen_mrs.add(ori_mrs);
			return this.gen_mrs;
		}

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
	protected MRS transformYNques () {
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
	protected ArrayList<MRS> transformWHques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
		ArrayList<EP> eps;

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
				EP hiEP, loEP;
				if (eps == null || eps.size() == 0) {
					continue;
				} else if (eps.size() == 1) {
					loEP = eps.get(0);
					lo = loEP.getLabel();
					// hiEP should be found through a qeq relation
					hi = q_mrs.getHiLabelFromHconsList(lo);
					if (hi == null) continue;
					ArrayList<EP> rstr = q_mrs.getEPbyFeatAndValue("RSTR", hi);
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
				} else {
					// one is hi, the other is lo in a qeq relation
					ArrayList<EP> hiloEPS = MRS.determineHiLowEP (eps, q_mrs);
					if (hiloEPS == null) continue;
					hiEP = hiloEPS.get(0);
					loEP = hiloEPS.get(1);
				}

				if (eps.size() > 2) {
					log.warn("the size of eps isn't 1 or 2 (but maybe I can manage): \n"+eps);
				}

				ArrayList<EP> removed = new ArrayList<EP>();
				for (EP ep:q_mrs.getEps()) {
					if (ep != loEP && ep.getLabel().equals(loEP.getLabel())
							&& !ep.getTypeName().toLowerCase().contains("_p_") && !ep.getTypeName().toLowerCase().contains("_v_")) {
						// possibly adjective, such as "next" in "next Monday".
						// this way is buggy, do it in DMRS
						removed.add(ep);
					}
				}
				if (removed.size() != 0) {
					q_mrs.removeEPlist(removed);
					log.warn("Removed EP with the same label as loEP:\n" + removed);
				}

				setupHiLoEP(q_mrs, hiEP, loEP, neType);

				if (neType.equals("NElocation")) {
					// NElocation generates two types of questions: where and which place
					// this scope of code generates the "which place" question
					MRS placeMrs = new MRS(q_mrs);
					// set hiEP to _WHICH_Q_REL and loEP to _place_n_of_rel
					placeMrs.getEPbyParallelIndex(q_mrs, hiEP).setTypeName("_WHICH_Q_REL");
					EP placeEP = placeMrs.getEPbyParallelIndex(q_mrs, loEP);
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
					EP ppEP = q_mrs.getEPbefore(term.getCfrom(), term.getCto());
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
				q_mrs.cleanHCONS();
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
	protected ArrayList<MRS> transformHOWques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
		ArrayList<EP> eps, hiloEPS;

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

					EP loEP;

					// one is hi, the other is lo in a qeq relation
					hiloEPS = MRS.determineHiLowEP (eps, q_mrs);
					if (hiloEPS == null) continue;
					loEP = hiloEPS.get(1);

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
					EP whichEP = new EP("WHICH_Q_REL", "h"+labelStore.get(0));
					String arg0 = "x"+labelStore.get(4);
					whichEP.addSimpleFvpair("ARG0", arg0);
					whichEP.addSimpleFvpair("RSTR", "h"+labelStore.get(1));
					whichEP.addSimpleFvpair("BODY", "h"+labelStore.get(2));
					EP abstrEP = new EP("ABSTR_DEG_REL", "h"+labelStore.get(3));
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
					EP measureEP = new EP("MEASURE_REL", loEP.getLabel());
					String[] extraPairs = {"SF", "PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"};
					measureEP.addFvpair("ARG0", "e"+labelStore.get(5), extraPairs);
					measureEP.addFvpair("ARG1", loEP.getArg0(), extraPairs);
					measureEP.addSimpleFvpair("ARG2", arg0);
					q_mrs.addEPtoEPS(measureEP);

					// change SF to "QUES"
					q_mrs.cleanHCONS();
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

	/**
	 * Set up <code>hiEP</code> and <code>loEP</code> according to
	 * the named entity type <code>neType</code>.
	 * @param q_mrs an MRS for a question
	 * @param hiEP a high EP in a qeq relation
	 * @param loEP a low EP in a qeq relation
	 * @param neType a named entity type
	 */
	protected void setupHiLoEP (MRS q_mrs, EP hiEP, EP loEP, String neType) {

		// change hiEP to which_q_rel
		hiEP.setTypeName("WHICH_Q_REL");

		// change loEP to person_rel
		if (neType.equals("NEperson")||neType.equals("NEfirstName")
				||neType.equals("NEmathematician")||neType.equals("NEplaywright")) {
			loEP.setTypeName("PERSON_REL");
			q_mrs.setSentType("WHO");

		} else if (neType.equals("NElocation")) {
			loEP.setTypeName("PLACE_N_REL");
			loEP.getValueVarByFeature("ARG0").setExtrapairValue("NUM", "SG");
			q_mrs.setSentType("WHERE");
//			loEP.setTypeName("THING_REL");
//			q_mrs.setSentType("WHAT");
		} else if (neType.equals("NEdate")||neType.equals("NEtime")||neType.equals("NEweekday")) {
			loEP.setTypeName("TIME_N_REL");
			q_mrs.setSentType("WHEN");

		} else {
			hiEP.setTypeName("_WHICH_Q_REL");
			q_mrs.setSentType("WHAT");
			if (neType.equals("NEanimal")) {
				loEP.setTypeName("_animal_n_1_rel");
			} else if (neType.equals("NEsport")) {
				loEP.setTypeName("_sport_n_1_rel");
			} else if (neType.equals("NEairport")) {
				loEP.setTypeName("_airport_n_1_rel");
			} else if (neType.equals("NEaward")) {
				loEP.setTypeName("_award_n_for_rel");
			} else if (neType.equals("NEbacteria")) {
				loEP.setTypeName("_bacteria_n_1_rel");
			} else if (neType.equals("NEbird")) {
				loEP.setTypeName("_bird_n_1_rel");
			} else if (neType.equals("NEbook")) {
				loEP.setTypeName("_book_n_of_rel");
			} else if (neType.equals("NEcanal")) {
				loEP.setTypeName("_canal_n_1_rel");
			} else if (neType.equals("NEcity")) {
				loEP.setTypeName("_city_n_1_rel");
			} else if (neType.equals("NEcompetition")) {
				loEP.setTypeName("_game_n_1_rel");
			} else if (neType.equals("NEconflict")) {
				loEP.setTypeName("_war_n_1_rel");
			} else if (neType.equals("NEcountry")) {
				loEP.setTypeName("_country_n_of_rel");
			} else if (neType.equals("NEdirector")) {
				loEP.setTypeName("_director_n_of_rel");
			} else if (neType.equals("NEdisease")) {
				loEP.setTypeName("_disease_n_1_rel");
			} else if (neType.equals("NEdrug")) {
				loEP.setTypeName("_drug_n_1_rel");
			} else if (neType.equals("NEeducationalInstitution")) {
				loEP.setTypeName("_school_n_1_rel");
			} else if (neType.equals("NEfestival")) {
				loEP.setTypeName("_festival_n_1_rel");
			} else if (neType.equals("NEfilm")) {
				loEP.setTypeName("_film_n_1_rel");
			} else if (neType.equals("NEflower")) {
				loEP.setTypeName("_flower_n_1_rel");
			} else if (neType.equals("NEisland")) {
				loEP.setTypeName("_island_n_1_rel");
			} else if (neType.equals("NElake")) {
				loEP.setTypeName("_lake_n_1_rel");
			} else if (neType.equals("NEmineral")) {
				loEP.setTypeName("_mineral_n_1_rel");
			} else if (neType.equals("NEmetal")) {
				loEP.setTypeName("_metal_n_1_rel");
			} else if (neType.equals("NEdepartment")) {
				loEP.setTypeName("_department_n_1_rel");
			} else if (neType.equals("NEministry")) {
				loEP.setTypeName("_ministry_n_1_rel");
			} else if (neType.equals("NEmountain")) {
				loEP.setTypeName("_mountain_n_1_rel");
			} else if (neType.equals("NEmusicalInstrument")) {
				loEP.setTypeName("_instrument_n_of_rel");
			} else if (neType.equals("NEmusical")) {
				loEP.setTypeName("_musical_n_1_rel");
			} else if (neType.equals("NEnarcotic")) {
				loEP.setTypeName("_drug_n_1_rel");
			} else if (neType.equals("NEnationalPark")) {
				loEP.setTypeName("_park_n_1_rel");
			} else if (neType.equals("NEocean")) {
				loEP.setTypeName("_ocean_n_1_rel");
			} else if (neType.equals("NEorganization")) {
				loEP.setTypeName("_organization_n_1_rel");
			} else if (neType.equals("NEpeninsula")) {
				loEP.setTypeName("_peninsula_n_1_rel");
			} else if (neType.equals("NEplant")) {
				loEP.setTypeName("_plant_n_1_rel");
			} else if (neType.equals("NEpoliticalParty")) {
				loEP.setTypeName("_party_n_of_rel");
			} else if (neType.equals("NEriver")) {
				loEP.setTypeName("_river_n_of_rel");
			} else if (neType.equals("NEscientist")) {
				loEP.setTypeName("_scientist_n_1_rel");
			} else if (neType.equals("NEsea")) {
				loEP.setTypeName("_sea_n_of_rel");
			} else if (neType.equals("NEsport")) {
				loEP.setTypeName("_sport_n_1_rel");
			} else if (neType.equals("NEstadium")) {
				loEP.setTypeName("_stadium_n_1_rel");
			} else if (neType.equals("NEstate")) {
				loEP.setTypeName("_state_n_of_rel");
			} else if (neType.equals("NEtherapy")) {
				loEP.setTypeName("_therapy_n_1_rel");
			} else if (neType.equals("NEusPresident")) {
				loEP.setTypeName("_president_n_of_rel");
			} else if (neType.equals("NEvirus")) {
				loEP.setTypeName("_virus_n_1_rel");
			} else if (neType.equals("NEsoftware")) {
				loEP.setTypeName("_software_n_1_rel");
			} else {
				hiEP.setTypeName("WHICH_Q_REL");
				loEP.setTypeName("THING_REL");
				// only keep "ARG0" as the feature
				loEP.keepFvpair(new String[]{"ARG0"});
				// also "ARG0" should be the same with hiEP
				loEP.setFvpairByFeatAndValue("ARG0", hiEP.getValueVarByFeature("ARG0"));
				q_mrs.setSentType("WHAT");
			}
			if (loEP.getTypeName().contains("_of_rel") || loEP.getTypeName().contains("_for_rel")) {
				// add an dumb ARG1 to "_of_rel" and "_for_rel"
				String value=q_mrs.generateUnusedLabel(1).get(0);
				loEP.addSimpleFvpair("ARG1", "i"+value);
			}
		}
		loEP.delFvpair("CARG");
		String[] extra = {"NUM", "PERS"};
		loEP.keepExtrapairInFvpair("ARG0", extra);
	}

}
