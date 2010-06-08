package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * Coordination decomposer. For coordinating conjunctions
 * (for, and, nor, but, or, yet, so) in English,
 * it finds "and"/"but"/"or" and decompose the sentence into (two)
 * different simpler ones. For instance:
 * "John likes Mary a little but hates Anna very much." ->
 * "John likes Mary a little. John hates Anna very much."
 * "John likes Anna a little and Peter hates Anna very much."
 * "John likes Anna a little. Peter hates Anna very much."
 *
 * @author Xuchen Yao
 *
 */
public class CoordDecomposer extends MrsDecomposer {

	private static Logger log = Logger.getLogger(CoordDecomposer.class);

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		if (inList == null) return null;

		String coordEPname = "_C_REL";

		ArrayList<MRS> outList = new ArrayList<MRS>();
		ElementaryPredication coordEP = null;

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				String pred = ep.getTypeName();
				if (pred.contains(coordEPname)) {
					String lEvent = ep.getValueByFeature("L-INDEX");
					String rEvent = ep.getValueByFeature("R-INDEX");

					for (String event: new String[]{lEvent, rEvent}) {
						MRS cMrs = new MRS(mrs);
						coordEP = cMrs.getEps().get(mrs.getEps().indexOf(ep));

						cMrs.keepDependentEPandVerbEP(cMrs.getCharVariableMap().get(event), coordEP);

						if (cMrs.removeEPbyFlag()) {
							cMrs.cleanHCONS();
							cMrs.changeFromUnkToNamed();
							cMrs.setDecomposer("Coordination");
							outList.add(cMrs);
						}
					}
					break;
				}

			}
		}

		return outList.size() == 0 ? null : outList;
	}


}
