/**
pipe: Mice do not like cats because cats catch mice.
pipe: Because cats catch mice mice do not like cats.
pipe: Why can't Kitty hear?
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 *
 */
public class WhyDecomposer extends MrsDecomposer {

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	@Override
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		if (inList == null) return null;
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS inMrs:inList) {

			ElementaryPredication becauseEP = null;
			String because = "_because_x_rel";
			String arg1Hi, arg2Hi, arg1Lo, arg2Lo;

			for (ElementaryPredication ep:inMrs.getEps()) {
				if (ep.getTypeName().equals(because)) {
					
					if (ep.getCfrom() > 0) {
						ArrayList<MRS> l = becauseMiddle (inMrs, ep);
						if (l!=null) outList.addAll(l);
					} else {
					}

					break;
				}
			}
		}
		
		return outList.size() == 0 ? null : outList;
	}
	
	protected ArrayList<MRS> becauseMiddle(MRS inMrs, ElementaryPredication becauseEP) {
		MRS reasonMrs = new MRS(inMrs);
		MRS resultMrs = new MRS(inMrs);
		ArrayList<MRS> list = new ArrayList<MRS>();
		
		String resultHi = becauseEP.getValueByFeature("ARG1");
		String reasonHi = becauseEP.getValueByFeature("ARG2");
		String resultLo = inMrs.getLoLabelFromHconsList(resultHi);
		String reasonLo = inMrs.getLoLabelFromHconsList(reasonHi);
		if (resultHi==null || reasonHi==null || resultLo==null || reasonLo==null) return null;
		
		
		boolean beforeBecause = true;
		
		for (int i=0; i<inMrs.getEps().size(); i++) {
			if (inMrs.getEps().get(i) == becauseEP) {
				reasonMrs.getEps().get(i).setFlag(true);
				resultMrs.getEps().get(i).setFlag(true);
				beforeBecause = false;
			}
			if (beforeBecause) {
				reasonMrs.getEps().get(i).setFlag(true);
			} else {
				resultMrs.getEps().get(i).setFlag(true);
			}
		}
		
		// the event index of resultMrs is inherited from the original MRS
		// we need to find out the event index for reason Mrs
		String reasonEvent = null;
		for (ElementaryPredication ep:reasonMrs.getEPbyLabelValue(reasonLo)) {
			if (ep.getArg0()!=null && ep.getArg0().startsWith("e")) {
				String tense = ep.getValueVarByFeature("ARG0").getExtrapair().get("TENSE");
				if (tense != null && !tense.equals("UNTENSED")) {
					reasonEvent = ep.getArg0();
					break;
				}
			}
		}
		
		if (reasonEvent != null) reasonMrs.setIndex(reasonEvent);
		
		if (reasonMrs.removeEPbyFlag()) {
			reasonMrs.cleanHCONS();
			reasonMrs.setDecomposer("WhyDecomposerReason");
			reasonMrs.changeFromUnkToNamed();
			list.add(reasonMrs);
		}
		
		if (resultMrs.removeEPbyFlag()) {
			resultMrs.cleanHCONS();
			resultMrs.setDecomposer("WhyDecomposerResult");
			resultMrs.changeFromUnkToNamed();
			list.add(resultMrs);
		}

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
		ElementaryPredication whichEP = new ElementaryPredication("WHICH_Q_REL", "h"+labelStore.get(0));
		String arg0 = "x"+labelStore.get(4);
		whichEP.addSimpleFvpair("ARG0", arg0);
		whichEP.addSimpleFvpair("RSTR", "h"+labelStore.get(1));
		whichEP.addSimpleFvpair("BODY", "h"+labelStore.get(2));
		ElementaryPredication reasonEP = new ElementaryPredication("REASON_REL", "h"+labelStore.get(3));
		reasonEP.addSimpleFvpair("ARG0", arg0);
		whyMrs.addEPtoEPS(whichEP);
		whyMrs.addEPtoEPS(reasonEP);
		whyMrs.addToHCONSsimple("qeq", "h"+labelStore.get(1), "h"+labelStore.get(3));
		
		// Generate _FOR_P_REL
		ElementaryPredication verbEP = whyMrs.getVerbEP();
		ElementaryPredication forEP = new ElementaryPredication("_FOR_P_REL", verbEP.getLabel());
		forEP.addSimpleFvpair("ARG0", "i"+labelStore.get(5));
		forEP.addSimpleFvpair("ARG1", verbEP.getValueByFeature("ARG0"));
		forEP.addSimpleFvpair("ARG2", arg0);
		
		whyMrs.addEPtoEPS(forEP);
		
		whyMrs.setAllSF2QUES();
		whyMrs.setSentType("WHY");
		whyMrs.changeFromUnkToNamed();
		// build cross references?
		whyMrs.buildCoref();
		list.add(whyMrs);
		
		return list.size()==0 ? null : list;
	}
	
	protected ArrayList<MRS> becauseFront(MRS inMrs, ElementaryPredication becauseEP) {
		MRS reasonMrs = new MRS(inMrs);
		MRS resultMrs = new MRS(inMrs);
		ArrayList<MRS> list = new ArrayList<MRS>();
		
		String resultHi = becauseEP.getValueByFeature("ARG1");
		String reasonHi = becauseEP.getValueByFeature("ARG2");
		String resultLo = inMrs.getLoLabelFromHconsList(resultHi);
		String reasonLo = inMrs.getLoLabelFromHconsList(reasonHi);
		if (resultHi==null || reasonHi==null || resultLo==null || reasonLo==null) return null;
		
		ElementaryPredication cutEP = null;
		
		return list.size()==0 ? null : list;
	}

}
