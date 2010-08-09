package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * Apposition decomposer. Apposition is a relationship between
 * two or more words in which the units are grammatically parallel
 * and have the same referent (e.g. my friend Sue) (Concise Oxford
 * English Dictionary).
 *
 * This class separates the ARG1 (my friend) and ARG2 (Sue) of an
 * APPOS_REL and generates two sentences with ARG1 and ARG2 individually
 * (My friend is a young. Sue is young. <-= My friend Sue is young.).
 * Then the NER could work better.
 *
 * Test sentences:
pipe: the girl Anna likes the dog Bart. (apposition)
pipe: The accident after Hurricane Katrina did not cause a civilizational collapse. (compound)

LTOP: h1
INDEX: e2
RELS: <
[ APPOS_REL<0:13>
  LBL: h3
  ARG0: e4 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE PROG: - PERF: - ]
  ARG1: x5 [ x PERS: 3 NUM: SG IND: + ]
  ARG2: x6 [ x PERS: 3 NUM: SG IND: + ]
]
[ _THE_Q_REL<0:3>
  LBL: h7
  ARG0: x5 [ x PERS: 3 NUM: SG IND: + ]
  RSTR: h9
  BODY: h8
]
[ _girl_n_1_rel<4:8>
  LBL: h10
  ARG0: x5 [ x PERS: 3 NUM: SG IND: + ]
]
[ PROPER_Q_REL<9:13>
  LBL: h11
  ARG0: x6 [ x PERS: 3 NUM: SG IND: + ]
  RSTR: h12
  BODY: h13
]
[ NAMED_REL<9:13>
  LBL: h14
  ARG0: x6 [ x PERS: 3 NUM: SG IND: + ]
  CARG: "Anna"
]
[ _like_v_1_rel<14:19>
  LBL: h3
  ARG0: e2 [ e SF: PROP TENSE: PRES MOOD: INDICATIVE PROG: - PERF: - ]
  ARG1: x5 [ x PERS: 3 NUM: SG IND: + ]
  ARG2: x15 [ x PERS: 3 NUM: SG IND: + ]
]
[ APPOS_REL<20:34>
  LBL: h3
  ARG0: e17 [ e SF: PROP TENSE: UNTENSED MOOD: INDICATIVE PROG: - PERF: - ]
  ARG1: x15 [ x PERS: 3 NUM: SG IND: + ]
  ARG2: x16 [ x PERS: 3 NUM: SG IND: + ]
]
[ _THE_Q_REL<20:23>
  LBL: h18
  ARG0: x15 [ x PERS: 3 NUM: SG IND: + ]
  RSTR: h20
  BODY: h19
]
[ _dog_n_1_rel<24:27>
  LBL: h21
  ARG0: x15 [ x PERS: 3 NUM: SG IND: + ]
]
[ PROPER_Q_REL<28:34>
  LBL: h22
  ARG0: x16 [ x PERS: 3 NUM: SG IND: + ]
  RSTR: h23
  BODY: h24
]
[ NAMED_REL<28:34>
  LBL: h25
  ARG0: x16 [ x PERS: 3 NUM: SG IND: + ]
  CARG: "Bart"
]
>
HCONS: < h9 qeq h10 h12 qeq h14 h20 qeq h21 h23 qeq h25 >
 *
 * @author Xuchen Yao
 *
 */
public class ApposDecomposer extends MrsDecomposer {

	private static Logger log = Logger.getLogger(ApposDecomposer.class);

	private String apposEPlabel = "APPOS_REL";
	private String compoundEPlabel = "COMPOUND_NAME_REL";

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {

		//String[] argList = new String[] {"ARG1", "ARG2"};
		if (inList == null) return null;

		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {

				String typeName = ep.getTypeName();

				// TODO: VERY IMPORTANT! multiple APPOS_REL!
				// see warnings in getEPbyTypeName() and getEPbyLabelValue().
				if (apposEPlabel.equals(typeName) || compoundEPlabel.equals(typeName)) {
					// Bingo! Found an apposition EP!

					// PART I: use this apposition to form a short sentence
					// for instance, "the girl Anna" -> "the girl is Anna."
					MRS m = assembleApposition2sent(mrs, ep);
					if (m != null) outList.add(m);

					// PART II: decompose the sentence by the first apposition.
					// for instance, "the girl Anna likes dogs."
					// -> "the girl like dogs." && "Anna likes dogs."
					ArrayList<MRS> l = divideApposition2sent(mrs, ep);
					if (l != null) outList.addAll(l);

					// this break is used only when this function is called
					// from the doIt(), which recursively calls decompose().
					break;
				}
			}
		}

		return outList.size() == 0 ? null : outList;
	}

	/**
	 * Use the apposition EP <code>apposEP</code> in <code>mrs</code> to
	 * form a short sentence. For instance, "the girl Anna" -> "the girl is Anna."
	 * @param mrs an input MRS
	 * @param apposEP the apposition EP, must be in <code>mrs</code>
	 * @return a new MRS representing a simple sentence formed by apposition
	 */
	public MRS assembleApposition2sent (MRS mrs, ElementaryPredication apposEP) {

		// There's obviously a better way to do this, which I omitted the first time.
		//MRS apposMrs = MRS.extractByEPandArg0(apposEP, mrs);
		MRS apposMrs = new MRS(mrs);
		apposEP = apposMrs.getEps().get(mrs.getEps().indexOf(apposEP));
		int cfrom = apposEP.getCfrom();
		int cto = apposEP.getCto();
		boolean inside = false;
		String oriTense = mrs.getTense();

		for (ElementaryPredication ep:apposMrs.getEps()) {
			// There are some EP without a range <cfrom:cto>, in this case,
			// if they are covered by the apposEP, don't remove it.
			if (ep.getCto() == -1 && ep.getCfrom() == -1 && inside) continue;
			if (ep.getCfrom() >= cfrom && ep.getCto() <= cto) {
				inside = true;
			} else {
				inside = false;
				ep.setFlag(true);
			}

		}
		if (!apposMrs.removeEPbyFlag()) return null;

		// It should contain only 1 EP. We have 3 steps here:
		// 1. change APPOS_REL to _BE_V_ID_REL
		// 2. change the main event of sentence to ARG0 of _BE_V_ID_REL
		// 3. change TENSE of the event to PRES
		// 4. change SF to QUES

		//ElementaryPredication newApposEP = apposMrs.getEPbyTypeName(apposEPlabel).get(0);
		// step 1
		apposEP.setTypeName("_BE_V_ID_REL");
		// step 2
		apposMrs.setIndex(apposEP.getArg0());
		// step 3
		apposEP.getValueVarByFeature("ARG0").setExtrapairValue("TENSE", oriTense);
		// step 4
		//apposMrs.setSF2QUES();
		apposMrs.setDecomposer("Apposition");
		apposMrs.changeFromUnkToNamed();
		for (ElementaryPredication ep:apposMrs.getEps()) {
			if (ep.getPred()!=null && ep.getTypeName().equalsIgnoreCase("UDEF_Q_REL")) {
				// Hurricane Katrina -> The hurricane is Katrina
				ep.setTypeName("_THE_Q_REL");
			}
		}
		apposMrs.cleanHCONS();

		return apposMrs;
	}

	/**
	 * Decompose the sentence by the first apposition EP <code>ep</code>.
	 * for instance, "the girl Anna likes dogs."
	 * -> "the girl like dogs." && "Anna likes dogs."
	 * @param mrs an input MRS
	 * @param apposEP the apposition EP, must be in <code>mrs</code>
	 * @return a new list of MRS representing a simple sentence formed by apposition
	 */
	public ArrayList<MRS> divideApposition2sent (MRS mrs, ElementaryPredication apposEP) {

		ArrayList<MRS> outList = new ArrayList<MRS>();

		String arg1Value = apposEP.getValueByFeature("ARG1");
		String arg2Value = apposEP.getValueByFeature("ARG2");

		if (arg1Value==null || arg2Value==null) {
			log.error("Error: APPOS_REL should have ARG1 and ARG2:\n"+apposEP);
			return null;
		}
		apposEP.setFlag(true);
		MRS arg1Mrs = new MRS(mrs);
		MRS arg2Mrs = new MRS(mrs);
		apposEP.setFlag(false);

		int cfrom = apposEP.getCfrom();
		int cto = apposEP.getCto();
		ElementaryPredication ep;
		boolean inside = false;
		boolean arg2Area = false;
		// remove all arg2 in arg1Mrs and all arg1 in arg2Mrs
		for (int i=0; i<mrs.getEps().size(); i++) {
			ep = mrs.getEps().get(i);
			if (ep.getCto() == -1 && ep.getCfrom() == -1 && inside) continue;
			if (ep.getCto() < cfrom) {
				inside = false;
			}
			else if (ep.getCfrom() > cto) {
				inside = false;
			}
			else {
				inside = true;
				/*
				 * We want to have a clear cut between arg1 and arg2 inside this apposition.
				 * This is a weak judgment: the EP with (ARG0: ARG1value) should be the head
				 * of the ARG1 phase, it's usually in the last position. So if its ARG0 value
				 * matches arg1Value, plus that it's not a hiLabel, then this is the end of
				 * arg1. Following is Arg2 so set arg2Area = true.
				 */
				if (arg2Area) {
					arg1Mrs.getEps().get(i).setFlag(true);
				} else {
					arg2Mrs.getEps().get(i).setFlag(true);
				}
				if (ep.getValueByFeature("ARG0").equals(arg1Value) &&
						ep.getValueByFeature("RSTR") == null) {
					arg2Area = true;
				}
			}
		}

		if(!arg1Mrs.removeEPbyFlag()) return null;
		if(!arg2Mrs.removeEPbyFlag()) return null;

		arg1Mrs.changeEPvalue(arg2Value, arg1Value);
		arg2Mrs.changeEPvalue(arg1Value, arg2Value);

		arg1Mrs.cleanHCONS();
		arg2Mrs.cleanHCONS();

		arg1Mrs.changeFromUnkToNamed();
		arg2Mrs.changeFromUnkToNamed();

		arg1Mrs.setDecomposer("Apposition");
		arg2Mrs.setDecomposer("Apposition");

		outList.add(arg1Mrs);
		outList.add(arg2Mrs);

		return outList;
	}

}

