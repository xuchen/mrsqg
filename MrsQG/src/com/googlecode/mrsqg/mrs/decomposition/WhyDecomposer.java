/**
pipe: Mice do not like cats because cats catch mice.
pipe: Because cats catch mice mice do not like cats.
pipe: Why can't Kitty hear?
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.EP;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 *
 */
public class WhyDecomposer extends MrsDecomposer {

	private static Logger log = Logger.getLogger(WhyDecomposer.class);

	private HashMap<String, Boolean> cueTypeNames;

	public WhyDecomposer() {
		cueTypeNames = new HashMap<String, Boolean>();
		// set it to true if ARG1 is result and ARG2 is reason, otherwise false
		cueTypeNames.put("_because_x_rel", true);
		cueTypeNames.put("_as_x_subord_rel", true);
		cueTypeNames.put("_in+order+to_x_rel", true);
		cueTypeNames.put("_DUE+TO_P_REL", true);
		cueTypeNames.put("_so+that_x_rel", false);
		cueTypeNames.put("_so_x_rel", false);
		cueTypeNames.put("_BECAUSE+OF_P_REL", true);
		cueTypeNames.put("_therefore_x_rel", false);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	@Override
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		if (inList == null) return null;
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS inMrs:inList) {

			for (EP ep:inMrs.getEps()) {
				if (cueTypeNames.containsKey(ep.getTypeName())) {
					ArrayList<MRS> l;
					l = becauseDecompose(inMrs, ep, cueTypeNames.get(ep.getTypeName()));
					if (l!=null) outList.addAll(l);

					break;
				}
			}
		}

		return outList.size() == 0 ? null : outList;
	}


	protected ArrayList<MRS> becauseDecompose(MRS inMrs, EP becauseEP, boolean exchange) {

		ArrayList<MRS> list = new ArrayList<MRS>();

		String resultHi, reasonHi;

		if (exchange) {
			resultHi = becauseEP.getValueByFeature("ARG1");
			reasonHi = becauseEP.getValueByFeature("ARG2");
		} else {
			resultHi = becauseEP.getValueByFeature("ARG2");
			reasonHi = becauseEP.getValueByFeature("ARG1");
		}

		String resultLo = inMrs.getLoLabelFromHconsList(resultHi);
		String reasonLo = null;
		if (reasonHi!=null)
			reasonLo = inMrs.getLoLabelFromHconsList(reasonHi);

		MRS reasonMrs = null;
		HashSet<EP> ansSet = null;
		if (reasonHi!=null || reasonLo!=null) {
			reasonMrs= new MRS(inMrs);
			ansSet = reasonMrs.doDecompositionByLabel(reasonHi, reasonMrs.getEPbyParallelIndex(inMrs, becauseEP), true, true);
		}
		MRS resultMrs = new MRS(inMrs);
		resultMrs.doDecompositionByLabel(resultHi, resultMrs.getEPbyParallelIndex(inMrs, becauseEP), true, true);

		if (reasonMrs!=null && reasonMrs.removeEPbyFlag(true)) {
			reasonMrs.cleanHCONS();
		}

		if (resultMrs.removeEPbyFlag(true)) {
			resultMrs.cleanHCONS();
			resultMrs.setAnsCrange(ansSet);
		}

		if (exchange && reasonMrs!=null) {
			// the event index of resultMrs is inherited from the original MRS
			// we need to find out the event index for the reasonMrs
			String reasonEvent = null;
			EP vEP = MRS.getDependentEP(reasonMrs.getEPbyLabelValue(reasonLo));
			if (vEP != null && vEP.getArg0()!=null && vEP.getArg0().startsWith("e")) {
				reasonEvent = vEP.getArg0();
			}
			if (reasonEvent != null) reasonMrs.setIndex(reasonEvent);
			else {
				log.error("Can't find the event index from label "+reasonLo+" for MRS:");
				log.error(reasonMrs);
			}
		} else {
			String resultEvent = null;
			EP vEP = MRS.getDependentEP(resultMrs.getEPbyLabelValue(resultLo));
			if (vEP != null && vEP.getArg0()!=null && vEP.getArg0().startsWith("e")) {
				resultEvent = vEP.getArg0();
			}
			if (resultEvent != null) resultMrs.setIndex(resultEvent);
			else {
				log.error("Can't find the event index from label "+resultLo+" for MRS:");
				log.error(resultMrs);
			}
		}

		if (reasonMrs!=null) {
			reasonMrs.setDecomposer("WhyDecomposerReason");
			list.add(reasonMrs);
		}
		resultMrs.setDecomposer("WhyDecomposerResult");

		list.add(resultMrs);

		MRS whyMrs = constructWhyMrs(resultMrs);
		if (whyMrs != null)
			list.add(whyMrs);

		return list.size()==0 ? null : list;
	}

	/**
	 * Given a result MRS, construct a why MRS from it. For instance:
	 * "mice don't like cats" -> "why don't mice like cats?"
	 * @param resultMrs
	 * @return an MRS for why questions
	 */
	public static MRS constructWhyMrs (MRS resultMrs) {
		/* Construct a why MRS
        [ _for_p_rel<0:1>
          LBL: h3
          ARG0: i5
          ARG1: e4 [ e SF: PROP-OR-QUES TENSE: UNTENSED MOOD: INDICATIVE PROG: - PERF: - ]
          ARG2: x6 ]
        [ which_q_rel
          LBL: h7
          ARG0: x6
          RSTR: h9
          BODY: h8 ]
        [ reason_rel<0:1>
          LBL: h10
          ARG0: x6 ]
		 */
		MRS whyMrs = new MRS(resultMrs);
		// Generate the WHICH_Q_REL qeq REASON_REL pair
		ArrayList<String> labelStore = whyMrs.generateUnusedLabel(6);
		if (labelStore == null) return null;
		EP whichEP = new EP("WHICH_Q_REL", "h"+labelStore.get(0));
		String arg0 = "x"+labelStore.get(4);
		whichEP.addSimpleFvpair("ARG0", arg0);
		whichEP.addSimpleFvpair("RSTR", "h"+labelStore.get(1));
		whichEP.addSimpleFvpair("BODY", "h"+labelStore.get(2));
		EP reasonEP = new EP("REASON_REL", "h"+labelStore.get(3));
		reasonEP.addSimpleFvpair("ARG0", arg0);
		whyMrs.addEPtoEPS(whichEP);
		whyMrs.addEPtoEPS(reasonEP);
		whyMrs.addToHCONSsimple("qeq", "h"+labelStore.get(1), "h"+labelStore.get(3));

		// Generate _FOR_P_REL
		EP verbEP = whyMrs.getVerbEP();
		if (verbEP == null) return null;
		EP forEP = new EP("_FOR_P_REL", verbEP.getLabel());
		forEP.addSimpleFvpair("ARG0", "i"+labelStore.get(5));
		forEP.addFvpair("ARG1", verbEP.getValueVarByFeature("ARG0"));
		forEP.addSimpleFvpair("ARG2", arg0);

		whyMrs.addEPtoEPS(forEP);

		whyMrs.setSF2QUES();
		whyMrs.setSentType("WHY");
		whyMrs.setDecomposer("WhyDecomposerWhy");
		whyMrs.changeFromUnkToNamed();
		// build cross references?
		whyMrs.postprocessing();

		return whyMrs;
	}
}
