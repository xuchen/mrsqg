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
import com.googlecode.mrsqg.nlp.Cheap;

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
		MRS q_mrs = transformYNques();

		this.gen_mrs.add(q_mrs);

		trMrsList = transformWHques(terms);
		if (trMrsList != null)
			this.gen_mrs.addAll(trMrsList);

		trMrsList = transformHowManyQues(terms);
		if (trMrsList != null)
			this.gen_mrs.addAll(trMrsList);

		trMrsList = transformHowQues();
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
	 * Transform from a declarative to WH (who/which/what/where/when) interrogative.
	 * @param terms all the terms in this MRS.
	 * @return an ArrayList of generated interrogative MRS, or null if none.
	 */
	public ArrayList<MRS> transformWHques (Term[] terms) {
		if (terms == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();
		MRS q_mrs;
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
		if (labels == null) return null;
		String hiLabel = "h"+labels.get(0), loLabel = "h"+labels.get(1),
			rstr = "h"+labels.get(2), body = "h"+labels.get(3), arg0value = "x"+labels.get(4);


		for (EP ep:this.ori_mrs.getEps()) {
			if (!ep.isVerbEP() && !ep.isPrepositionEP()) {
				// we are only interested in the arguments of verbEP or ppEP
				continue;
			}
			for (DMRS dmrs:ep.getDmrsSet()) {
				dEP = dmrs.getEP();
				if ( !(dmrs.getPreSlash() == DMRS.PRE_SLASH.ARG && dmrs.getDirection() == DMRS.DIRECTION.DEP &&
						(dmrs.getPostSlash() == DMRS.POST_SLASH.NEQ || dmrs.getPostSlash() == DMRS.POST_SLASH.H)))
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

				if (!q_mrs.removeEPbyFlag(false))
					continue;

				q_mrs.cleanHCONS();
				if (gEP.isPrepositionEP()) {
					//remove the preposition to set the answer term more correctly
					dEPS.remove(gEP);
				}
				q_mrs.setAnsCrange(dEPS);

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
		q_mrs = removeFocusD(q_mrs);

		if (neType.equals("NElocation") && gEP.isPrepositionEP()) {
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
		outList.add(q_mrs);

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

	public ArrayList<MRS> transformHowManyQues (Term[] terms) {
		if (terms == null) return null;
		ArrayList<MRS> outList = new ArrayList<MRS>();

		/**
		 * How many/much/long questions need to add too many EPs which makes
		 * the transformed MRS pretty fragile. Thus we only do lexical substitution
		 * here to obtain the transformed MRS.
		 */
		/*
		 * the one hundred twenty apples are there.
		 * The one hundred men have 5 apples.
		 * numbers such as above can't be detected by NER
		 * we need to process CARD_REL
		 */
		EP ep, nounEP;
		HashSet<EP> cardEPS = new HashSet<EP>();

		int cto = -1, size = this.ori_mrs.getEps().size();
		boolean gotIt = false;
		for (int i=0; i<size; i++) {
			ep = this.ori_mrs.getEps().get(i);
			if (ep.getTypeName().contains("CARD_REL")) {
				// CARD_REL, BASIC_CARD_REL
				if (cto == -1) {
					// the first CARD_REL
					cto = ep.getCto();
					cardEPS.clear();
				} else if (cto != -1 && ep.getCfrom() == cto + 1) {
					// any consecutive CARD_REL
					cto = ep.getCto();
				} else {
					// a new CARD_REL
					cto = ep.getCto();
					cardEPS.clear();
				}
				cardEPS.add(ep);

				gotIt = false;
			} else {
				if (cto != -1 && cto != ep.getCto()) {
					gotIt = true;
					cto = -1;
				} else {
					gotIt = false;
				}
			}

			if (gotIt == false) {
				continue;
			}

			MRS qMrs = new MRS(this.ori_mrs);
			cardEPS = qMrs.getEPSbyParallelIndex(this.ori_mrs, cardEPS);
			HashSet<EP> nounEPS = new HashSet<EP>();
			// got a set of CARD_REL
			for (EP e:cardEPS) {
				/*
				 *  find out the noun EP those card_rel modifies
				 *  such as people from "one hundred" people
				 */
				for (DMRS dmrs:e.getDmrsSet()) {
					if (dmrs.getDirection()==DMRS.DIRECTION.DEP &&
							dmrs.getPreSlash()==DMRS.PRE_SLASH.ARG &&
							(dmrs.getPostSlash()==DMRS.POST_SLASH.NEQ || dmrs.getPostSlash()==DMRS.POST_SLASH.EQ)) {
						nounEPS.add(dmrs.getEP());
					}
				}
			}
			if (nounEPS.size() != 1) {
				log.error("The set of EPS modified by CARD_REL should only contain one EP:"+nounEPS);
				log.error("DEBUG YOUR CODE!");
				cardEPS.clear();
				continue;
			}
			HashSet<EP> ansEPS = qMrs.doDecomposition(cardEPS, nounEPS, false, true);
			// remove all cardEP related EPs
			if (!qMrs.removeEPbyFlag(false)) continue;
			qMrs.setAnsCrange(ansEPS);

			nounEP = (EP)nounEPS.toArray()[0];
			/*
			 * change the quantifier of nounEP to UDEF_Q_REL
			 * this is to avoid generate sth. like "the how many apples are there"
			 */
			for (DMRS dmrs:nounEP.getDmrsSet()) {
				if (dmrs.getPreSlash()==DMRS.PRE_SLASH.RSTR && dmrs.getPostSlash()==DMRS.POST_SLASH.H) {
					dmrs.getEP().setTypeName("UDEF_Q_REL");
				}
			}
			/*
[ ABSTR_DEG_REL<0:3>
  LBL: h7
  ARG0: x8
]
        DMRS: [ <-RSTR/H-- WHICH_Q_REL,  <-ARG2/NEQ-- MEASURE_REL]
[ WHICH_Q_REL<0:3>
  LBL: h9
  ARG0: x8
  RSTR: h11
  BODY: h10
]
        DMRS: [ --RSTR/H-> ABSTR_DEG_REL]
[ MEASURE_REL<0:3>
  LBL: h12
  ARG0: e13 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]
  ARG1: e14 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]
  ARG2: x8
]
        DMRS: [ --ARG1/EQ-> MUCH-MANY_A_REL,  --ARG2/NEQ-> ABSTR_DEG_REL]
[ MUCH-MANY_A_REL<4:8>
  LBL: h12
  ARG0: e14 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE ]
  ARG1: x4 [ x PERS: 3 NUM: PL IND: + ]
]
        DMRS: [ --ARG1/EQ-> _apple_n_1_rel,  <-ARG1/EQ-- MEASURE_REL]
[ _apple_n_1_rel<9:15>
  LBL: h12
  ARG0: x4 [ x PERS: 3 NUM: PL IND: + ]
]
        DMRS: [ <-RSTR/H-- UDEF_Q_REL,  <-ARG1/NEQ-- LOC_NONSP_REL,  <-ARG1/EQ-- MUCH-MANY_A_REL]
HCONS: < h5 qeq h12 h11 qeq h7 >
			 */
			ArrayList<String> labelStore = qMrs.generateUnusedLabel(7);
			String h7="h"+labelStore.get(0), x8="x"+labelStore.get(1),
				h9="h"+labelStore.get(2), h10="h"+labelStore.get(3),
				h11="h"+labelStore.get(4),
				e13="e"+labelStore.get(5), e14="e"+labelStore.get(6);
			String h12 = nounEP.getLabel();

			EP abstrEP = new EP("ABSTR_DEG_REL", h7);
			abstrEP.addSimpleFvpair("ARG0", x8);

			EP whichEP = new EP("WHICH_Q_REL", h9);
			whichEP.addSimpleFvpair("ARG0", x8);
			whichEP.addSimpleFvpair("RSTR", h11);
			whichEP.addSimpleFvpair("BODY", h10);

			qMrs.addEPtoEPS(abstrEP);
			qMrs.addEPtoEPS(whichEP);
			qMrs.addToHCONSsimple("qeq", h11, h7);

			EP measureEP = new EP("MEASURE_REL", h12);
			String[] extraPairs0 = {"SF","PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"};
			Var var0 = new Var(e13, extraPairs0);
			measureEP.addFvpair("ARG0", var0);
			String[] extraPairs1 = {"SF","PROP", "TENSE", "UNTENSED", "MOOD", "INDICATIVE"};
			Var var1 = new Var(e14, extraPairs1);
			measureEP.addFvpair("ARG1", var1);
			measureEP.addSimpleFvpair("ARG2", x8);

			EP muchEP = new EP("MUCH-MANY_A_REL", h12);
			muchEP.addFvpair("ARG0", var1);
			Var x4Var = nounEP.getValueVarByFeature("ARG0");
			muchEP.addFvpair("ARG1", x4Var);

			qMrs.addEPtoEPS(measureEP);
			qMrs.addEPtoEPS(muchEP);

			qMrs = removeFocusD(qMrs);
			qMrs.cleanHCONS();

			qMrs.setSF2QUES();
			qMrs.setSentType("HOW MANY/MUCH");
			qMrs.changeFromUnkToNamed();
			qMrs.postprocessing();
			outList.add(qMrs);
		}

		return outList.size() == 0 ? null : outList;
	}

	protected MRS removeFocusD(MRS mrs) {
		/*
		 * The first "FOCUS_D_REL" relation which covers the whole
		 * sentence must be removed otherwise no generation
		 * In 1996, the trust employed over 7,000 staff
		 */
		ArrayList<EP> list = mrs.getEps();
		int clast = -1;
		for (EP ep:list) {
			if (ep.getCto() > clast) clast = ep.getCto();
		}
		if (list.get(0).getTypeName().equals("FOCUS_D_REL") &&
				list.get(0).getCto()==clast) {
			mrs.removeEP(list.get(0));
		}

		return mrs;
	}

	public ArrayList<MRS> transformHowQues () {
		ArrayList<MRS> outList = new ArrayList<MRS>();

		/**
		 * six entries in core.smi:
		 * _by+means+of_p_rel : ARG0 e, ARG1 u, ARG2 i.
  		 * _by+way+of_p_rel : ARG0 e, ARG1 u, ARG2 i.
  		 * _by_p_means_rel : ARG0 e, ARG1 u, ARG2 i.
  		 * _by_p_n-n_rel : ARG0 e, ARG1 u, ARG2 x.
  		 * _by_p_rel : ARG0 e, ARG1 u, [ ARG2 i ].
  		 * _by_p_temp_rel : ARG0 e, ARG1 u, ARG2 i.
		 *
		 * we are interested in _by_p_means_rel, _by+means+of_p_rel, _by+way+of_p_rel,
		 * _by_p_rel (not very accurate)
		 */
		HashSet<String> bySet = new HashSet<String>();
		bySet.add("_BY_P_MEANS_REL");
		bySet.add("_BY_P_REL");
		bySet.add("_BY+WAY+OF_P_REL");
		bySet.add("_BY+MEANS+OF_P_REL");

		EP byEP, tEP;

		for (EP ep:this.ori_mrs.getEps()) {
			tEP = null;
			if (!bySet.contains(ep.getTypeName())) {
				continue;
			}
			for (DMRS dmrs:ep.getDmrsSet()) {
				// the means of by is indexed by an ARG2/NEQ relation
				if (dmrs.getDirection() == DMRS.DIRECTION.DEP &&
						dmrs.getPreSlash() == DMRS.PRE_SLASH.ARG &&
						dmrs.getPostSlash() == DMRS.POST_SLASH.NEQ &&
						dmrs.getArgNum().equals("2")) {
					tEP = dmrs.getEP();
					break;
				}
			}
			if (tEP == null) continue;
			MRS qMrs = new MRS(this.ori_mrs);
			byEP = qMrs.getEPbyParallelIndex(this.ori_mrs, ep);
			tEP = qMrs.getEPbyParallelIndex(this.ori_mrs, tEP);

			// removed everything indexed by byEP
			HashSet<EP> ansEPS = qMrs.doDecompositionbyEP(tEP, byEP, false, true);
			if (!qMrs.removeEPbyFlag(false)) continue;
			qMrs.setAnsCrange(ansEPS);

			/*
[ UNSPEC_MANNER_REL<11:16>
  LBL: h8
  ARG0: e17 [ e SF: PROP ]
  ARG1: e2 [ e SF: QUES TENSE: PRES MOOD: INDICATIVE PROG: - PERF: - ]
  ARG2: x16
]
        DMRS: [ --ARG1/EQ-> _go_v_1_rel,  --ARG2/NEQ-> MANNER_REL]
[ WHICH_Q_REL<11:16>
  LBL: h18
  ARG0: x16
  RSTR: h20
  BODY: h19
]
        DMRS: [ --RSTR/H-> MANNER_REL]
[ MANNER_REL<11:16>
  LBL: h21
  ARG0: x16
]
        DMRS: [ <-RSTR/H-- WHICH_Q_REL,  <-ARG2/NEQ-- UNSPEC_MANNER_REL]
>
HCONS: < ... h20 qeq h21 >

			 */
			ArrayList<String> labelStore = qMrs.generateUnusedLabel(5);
			String x16="x"+labelStore.get(0), h18="h"+labelStore.get(1),
				h20="h"+labelStore.get(2), h19="h"+labelStore.get(3),
				h21="h"+labelStore.get(4);

			byEP.setTypeName("UNSPEC_MANNER_REL");
			byEP.setSimpleFvpairByFeatAndValue("ARG2", x16);

			EP whichEP = new EP("WHICH_Q_REL", h18);
			whichEP.addSimpleFvpair("ARG0", x16);
			whichEP.addSimpleFvpair("RSTR", h20);
			whichEP.addSimpleFvpair("BODY", h19);

			EP mannerEP = new EP("MANNER_REL", h21);
			mannerEP.addSimpleFvpair("ARG0", x16);

			qMrs.addEPtoEPS(whichEP);
			qMrs.addEPtoEPS(mannerEP);
			qMrs.addToHCONSsimple("qeq", h20, h21);
			qMrs.cleanHCONS();

			qMrs.setSF2QUES();
			qMrs.setSentType("HOW");
			qMrs.changeFromUnkToNamed();
			qMrs.postprocessing();
			outList.add(qMrs);
		}

		return outList.size() == 0 ? null : outList;
	}
}
