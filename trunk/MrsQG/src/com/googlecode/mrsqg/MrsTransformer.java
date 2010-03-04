package com.googlecode.mrsqg;

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
	
	public MrsTransformer (String file, Preprocessor p) {
		this.ori_mrs = new MRS(file);
		this.pre = p;
		this.gen_mrs = new ArrayList<MRS>();
	}
	
	public void transform () {
		Term[] terms = pre.getTerms()[0];
		if (terms == null) return;
		
		String neType;
		ArrayList<ElementaryPredication> eps;
		
		for (Term term:terms) {
			neType = Arrays.toString(term.getNeTypes());
			if (neType.length()==0) {
				log.error("NE types shouldn't be none: "+term);
			}
			
			if (neType.contains("NEperson")) {
				MRS q_mrs = new MRS(ori_mrs);
				eps = q_mrs.getEPS(term.getCfrom(), term.getCto());
				if (eps.size() != 2) {
					log.error("the size of eps isn't 2: "+eps);
					return;
				}
				// one is hi, the other is lo in a qeq relation
				String hi, lo, rstr;
				ElementaryPredication hiEP, loEP;
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
						return;
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
							return;
						}
					}
				}
				if (!match) {
					log.error("hi "+hi+" and lo "+lo+" don't match" +
							" with HCONS: "+q_mrs.getHcons());
					return;
				}
				
				// change hiEP to which_q_rel
				hiEP.setPred("WHICH_Q_REL");
				
				// change loEP to person_rel
				loEP.setPred("PERSON_REL");
				loEP.delFvpair("CARG");
				String[] extra = {"NUM", "PERS"};
				loEP.keepExtrapairInFvpair("ARG0", extra);
				
				// change SF to "QUES"
				// e2
				String index = q_mrs.getIndex();
				FvPair v = q_mrs.getFvpairByRargnameAndIndex("ARG0", index);
				if (v==null) {
					log.error("FvPair ARG0: "+index+" not found! " +
							"can't set SF to QUES!");
					break;
				}
				v.getVar().setExtrapairValue("SF", "QUES");
				
				
				this.gen_mrs.add(q_mrs);
				q_mrs.printXML();
				System.out.println(q_mrs);

			}
		}
	}

	public static void main(String[] args) {

	}

}
