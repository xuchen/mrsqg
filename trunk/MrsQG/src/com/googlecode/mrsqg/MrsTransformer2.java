/**
 *
 */
package com.googlecode.mrsqg;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.DMRS;
import com.googlecode.mrsqg.mrs.EP;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.mrs.Var;

/**
 * This is the sequel of the original Mrs. Transformer,
 * aka, MrsTransformer2: Revenge of the Dependencies. This new generation of
 * Transformer is equipped with a more productive and upgraded killer technique:
 * Dependency Minimal Recursion Semantics.
 * @author Xuchen Yao
 * @version 2010-08-12
 */
public class MrsTransformer2 extends MrsTransformer {

	private static Logger log = Logger.getLogger(MrsTransformer2.class);

	/**
	 * @param file
	 * @param p
	 */
	public MrsTransformer2(File file, Preprocessor p) {
		super(file, p);
	}

	/**
	 * @param mrx
	 * @param p
	 */
	public MrsTransformer2(String mrx, Preprocessor p) {
		super(mrx, p);
	}

	/**
	 * @param mrs
	 * @param p
	 */
	public MrsTransformer2(MRS mrs, Preprocessor p) {
		super(mrs, p);
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
		//MRS q_mrs = transformYNques();

		//this.gen_mrs.add(q_mrs);

		trMrsList = transformWHques(terms);
		if (trMrsList != null)
			this.gen_mrs.addAll(trMrsList);

//		trMrsList = transformHOWques(terms);
//		if (trMrsList != null)
//			this.gen_mrs.addAll(trMrsList);

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
	 * Transform from a declarative to WH (who/which/what/where/when) interrogative.
	 * @param terms all the terms in this MRS.
	 * @return an ArrayList of generated interrogative MRS, or null if none.
	 */
	public ArrayList<MRS> transformWHques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
		ArrayList<EP> eps;
		// dependency EP and governor EP
		EP dEP, gEP;
		// the set of EPs related with dEP
		HashSet<EP> dEPS;
		// newly constructed hiEP and loEP
		EP hiEP, loEP;
		HashSet<String> neTypes;
		Var var;

		/*
[ WHICH_Q_REL<14:20>
  LBL: h15
  ARG0: x13
  RSTR: h17
  BODY: h16
]
[ THING_REL<14:20>
  LBL: h14
  ARG0: x13
]
HCONS: < ...h17 qeq h14 >
		 */
		ArrayList<String> labels = this.ori_mrs.generateUnusedLabel(5);
		String hiLabel = "h"+labels.get(0), loLabel = "h"+labels.get(1),
			rstr = "h"+labels.get(2), body = "h"+labels.get(3), arg0value = "x"+labels.get(4);


		for (EP ep:this.ori_mrs.getEps()) {
			if (!ep.isVerbEP() && !ep.isPrepositionEP()) {
				// we are only interested in the arguments of verbEP or ppEP
				continue;
			}
			for (DMRS dmrs:ep.getDmrsSet()) {
				dEP = dmrs.getEP();
				if ( !(dmrs.getPreSlash() == DMRS.PRE_SLASH.ARG && (dmrs.getPostSlash() == DMRS.POST_SLASH.NEQ || dmrs.getPostSlash() == DMRS.POST_SLASH.H)))
					continue;
				q_mrs = new MRS(this.ori_mrs);
				dEP = q_mrs.getEPbyParallelIndex(this.ori_mrs, dEP);
				gEP = q_mrs.getEPbyParallelIndex(this.ori_mrs, ep);

				dEPS = q_mrs.doDecompositionbyEP(dEP, gEP, false, true);

				hiEP = new EP("WHICH_Q_REL", hiLabel);
				loEP = new EP("THING_REL", loLabel);
				if (dmrs.getPostSlash() == DMRS.POST_SLASH.H) {
					/*
					 * A rare case of qeq relation, such as "John told Peter he loves Mary ."
[ _tell_v_1_rel<5:9>
  LBL: h8
  ARG0: e2 [ e SF: PROP TENSE: PAST MOOD: INDICATIVE PROG: - PERF: - ]
  ARG1: x6 [ x PERS: 3 NUM: SG GEND: M IND: + ]
  ARG2: x9 [ x PERS: 3 NUM: SG IND: + ]
  ARG3: h10
]
    DMRS: [ --ARG3/H-> _love_v_1_rel,  --ARG2/NEQ-> NAMED_REL(Peter),  --ARG1/NEQ-> NAMED_REL(John)]
					 *
					 * we don't want "h10" to be the ARG0 value, so generate a new 'x' one
					 */
					hiEP.addSimpleFvpair("ARG0", arg0value);
					loEP.addSimpleFvpair("ARG0", arg0value);
					gEP.setSimpleFvpairByFeatAndValue("ARG"+dmrs.getArgNum(), arg0value);
				} else {
					// inherit the extra feature value pair to Arg0
					var = gEP.getValueVarByFeature("ARG"+dmrs.getArgNum());
					hiEP.addFvpair("ARG0", var);
					loEP.addFvpair("ARG0", var);
				}

				hiEP.addSimpleFvpair("RSTR", rstr);
				hiEP.addSimpleFvpair("BODY", body);

				q_mrs.addToHCONSsimple("qeq", rstr, loLabel);

				hiEP.setFlag(true);
				loEP.setFlag(true);


				// must add them before calling setupHiLoEP(), otherwise
				// generateUnusedLabel() in setupHiLoEP() will fail
				q_mrs.addEPtoEPS(hiEP);
				q_mrs.addEPtoEPS(loEP);

				neTypes = neTypeInEPS(dEPS, terms);

				if (neTypes.size() > 1) {
					// generate a "what" question for multiple neTypes
					neTypes.add("");
				}

				if (neTypes.size() == 0) {
					// generate a "what" question if no NEs are found
					outList.addAll(setupHiLoEPAll(q_mrs, hiEP, loEP, "", gEP));
				} else {
					for (String neType:neTypes) {
						MRS qMrs = new MRS(q_mrs);
						EP qhiEP = qMrs.getEPbyParallelIndex(q_mrs, hiEP);
						EP qloEP = qMrs.getEPbyParallelIndex(q_mrs, loEP);
						EP qgEP = qMrs.getEPbyParallelIndex(q_mrs, gEP);
						outList.addAll(setupHiLoEPAll(qMrs, qhiEP, qloEP, neType, qgEP));
					}
				}

			}
		}


		return outList.size() == 0 ? null : outList;
	}

	protected ArrayList<MRS> setupHiLoEPAll (MRS q_mrs, EP hiEP, EP loEP, String neType, EP gEP) {
		ArrayList<MRS> outList = new ArrayList<MRS>();

		setupHiLoEP(q_mrs, hiEP, loEP, neType);

		if (neType.equals("NElocation") && !gEP.isPrepositionEP()) {
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

		if (gEP.isPrepositionEP() && (neType.equals("NElocation") || neType.equals("NEdate")))
		{
			EP ppEP = gEP;
			// change the preposition (if any) before the term

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

		// change SF to "QUES"
		q_mrs.setSF2QUES();
		q_mrs.changeFromUnkToNamed();
		if(q_mrs.removeEPbyFlag(false)) {
			outList.add(q_mrs);
		} else
			return outList;


		return outList;

	}

	/**
	 * Find out all named entity types in <code>eps</code> given <code>terms</code>
	 * @param eps
	 * @param terms
	 * @return a set of neType
	 */
	public static HashSet<String> neTypeInEPS (HashSet<EP> eps, Term[] terms) {
		HashSet<String> neTypes = new HashSet<String>();
		int cFrom=Integer.MAX_VALUE, cTo=Integer.MIN_VALUE;

		// the character range of eps
		for (EP ep:eps) {
			if (ep.getCfrom() < cFrom) cFrom = ep.getCfrom();
			if (ep.getCto() > cTo) cTo = ep.getCto();
		}
		for (Term term:terms) {
			if (term.getCfrom()>=cFrom && term.getCto()<=cTo)
				neTypes.addAll(Arrays.asList(term.getNeTypes()));
		}

		return neTypes;
	}
}
