package com.googlecode.mrsqg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.FvPair;
import com.googlecode.mrsqg.mrs.HCONS;
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
	
	public ArrayList<MRS> transform (boolean print) {
		//ArrayList<MRS> trMrsList = new ArrayList<MRS>();
		Term[] terms = pre.getTerms()[0];
		
		ArrayList<ElementaryPredication> eps;
		
		// generate yes/no question
		// change SF to "QUES"
		// e2
		MRS q_mrs = new MRS(ori_mrs);
		String index = q_mrs.getIndex();
		FvPair v = q_mrs.getFvpairByRargnameAndIndex("ARG0", index);
		if (v==null) {
			log.error("FvPair ARG0: "+index+" not found! " +
					"can't set SF to QUES!");
		}
		v.getVar().setExtrapairValue("SF", "QUES");
		
		q_mrs.changeFromUnkToNamed();
		q_mrs.setSentForce("Y/N");
		q_mrs.setAllSF2QUES();
		this.gen_mrs.add(q_mrs);
		if (print) {
			log.info("yes/no question MRX:");
			log.info(q_mrs.toMRXstring());
			log.info(q_mrs);
		}
		
		if (terms == null) return this.gen_mrs;
		
		for (Term term:terms) {
			for (String neType:term.getNeTypes()) {
				//neType = Arrays.toString(term.getNeTypes());
				if (neType.length()==0) {
					log.error("NE types shouldn't be none: "+term);
				}
				
				//if (neType.contains("NEperson")||neType.contains("NElocation")||neType.contains("NEdate"))
				q_mrs = new MRS(ori_mrs);
				eps = q_mrs.getEPS(term.getCfrom(), term.getCto());
				String hi, lo, rstr;
				ElementaryPredication hiEP, loEP;
				if (eps.size() == 1) {
					loEP = eps.get(0);
					lo = loEP.getLabel();
					// hiEP should be found through a qeq relation
					hi = MRS.getHiLabelFromHconsList(lo, q_mrs.getHcons());
					hiEP = q_mrs.getEPbyRargnameAndIndex("RSTR", hi);
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

					hiEP = loEP = eps.get(0);
					hi = lo = eps.get(0).getLabel();
					rstr = eps.get(1).getVarLabel("RSTR");
					if (rstr == null) {
						loEP = eps.get(1);
						lo = loEP.getLabel();
						hi = hiEP.getVarLabel("RSTR");
					} else {
						hiEP = eps.get(1);
						hi = rstr;
						try {
							assert lo == rstr;
						} catch (AssertionError e) {
							log.error("In eps:\n"+eps+"\none should refer" +
							"the other in RSTR field");
							continue;
						}
					}
					// check whether hi and lo match HCONS
					boolean match = false;
					for (HCONS h: q_mrs.getHcons()) {
						if (h.getHi().equals(hi)) {
							try {
								assert h.getLo().equals(lo);
								assert h.getRel().equals("qeq");
								match = true;
								break;
							} catch (AssertionError e) {
								log.error("hi "+hi+" and lo "+lo+" don't match" +
										" with HCONS: "+h);
								continue;
							}
						}
					}
					if (!match) {
						log.error("hi "+hi+" and lo "+lo+" don't match" +
								" with HCONS: "+q_mrs.getHcons());
						continue;
					}
				} else {
					log.error("the size of eps isn't 1 or 2: "+eps);
					continue;
				}

				// change hiEP to which_q_rel
				hiEP.setPred("WHICH_Q_REL");

				// change loEP to person_rel
				if (neType.equals("NEperson")||neType.equals("NEfirstName")) {
					loEP.setPred("PERSON_REL");
					q_mrs.setSentForce("WHO");
					if (print) {
						log.info("who question MRX:");
					}
				} else if (neType.equals("NElocation")) {
//					loEP.setPred("PLACE_N_REL");
//					q_mrs.setSentForce("WHERE");
					loEP.setPred("THING_REL");
					q_mrs.setSentForce("WHAT");
					if (print) {
						log.info("what question MRX:");
					}
				} else if (neType.equals("NEdate")) {
					loEP.setPred("TIME_N_REL");
					q_mrs.setSentForce("WHEN");
					if (print) {
						log.info("when question MRX:");
					}
				} else {
					loEP.setPred("THING_REL");
					q_mrs.setSentForce("WHAT");
					if (print) {
						log.info("what question MRX:");
					}
				}
				if (neType.equals("NElocation") || neType.equals("NEdate"))
				{
					ElementaryPredication ppEP = q_mrs.getEPbefore(term.getCfrom(), term.getCto());
					String pp = this.pre.getPrepositionBeforeTerm(term, 0);
					// change the preposition (if any) before the term
					if (pp!=null && ppEP.getPred()!=null && 
							ppEP.getPred().substring(0, pp.length()+1).toLowerCase().contains(pp.toLowerCase())) {
						// the Pred of an "in" preposition EP is something like: _in_p_
						// so the first 3 chars _in must contain "in"
						ppEP.setPred("LOC_NONSP_REL");
						loEP.setPred("PLACE_N_REL");
						q_mrs.setSentForce("WHERE");
						if (print) {
							log.info("what question MRX:");
						}
					}

				}
				loEP.delFvpair("CARG");
				String[] extra = {"NUM", "PERS"};
				loEP.keepExtrapairInFvpair("ARG0", extra);

				// change SF to "QUES"
				q_mrs.setAllSF2QUES();
				// e2
//				index = q_mrs.getIndex();
//				v = q_mrs.getFvpairByRargnameAndIndex("ARG0", index);
//				if (v==null) {
//					log.error("FvPair ARG0: "+index+" not found! " +
//					"can't set SF to QUES!");
//					continue;
//				}
//				v.getVar().setExtrapairValue("SF", "QUES");

				q_mrs.changeFromUnkToNamed();
				this.gen_mrs.add(q_mrs);
				if (print) {
					log.info(q_mrs.toMRXstring());
					log.info(q_mrs);
				}

			}
		}
		return this.gen_mrs;
	}

	public static void main(String[] args) {

	}

}
