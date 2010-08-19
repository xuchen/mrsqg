package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.EP;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * Coordination decomposer. For coordinating conjunctions
 * (for, and, nor, but, or, yet, so) in English,
 * it finds "and"/"but"/"or" and decompose the sentence into (two)
 * different simpler ones. For instance:
<pre>
 * "John likes Mary a little but hates Anna very much." ->
 * "John likes Mary a little. John hates Anna very much."
 * "John likes Anna a little and Peter hates Anna very much."
 * "John likes Anna a little. Peter hates Anna very much."
</pre>
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
		EP coordEP = null;

		for (MRS mrs:inList) {
			for (EP ep:mrs.getEps()) {
				String pred = ep.getTypeName();
				if (pred.contains(coordEPname)) {
					String lEvent = ep.getValueByFeature("L-INDEX");
					String rEvent = ep.getValueByFeature("R-INDEX");

					if ((lEvent == null || rEvent == null) || (lEvent.startsWith("x") && rEvent.startsWith("x"))) {
						// don't decompose coordination of NPs
						continue;
					}

					for (String event: new String[]{lEvent, rEvent}) {
						// could be empty index such as from a _BUT_C_REL "conjunction"
						if (event.startsWith("i") || event.startsWith("u")) continue;
						MRS cMrs = new MRS(mrs);
						coordEP = cMrs.getEps().get(mrs.getEps().indexOf(ep));

						//cMrs.keepDependentEPandVerbEP(cMrs.getCharVariableMap().get(event), coordEP);
						cMrs.doDecomposition(new HashSet<EP>(Arrays.asList(cMrs.getCharVariableMap().get(event))),
								new HashSet<EP>(Arrays.asList(coordEP)), true, true);

						if (cMrs.removeEPbyFlag(true)) {
							cMrs.setIndex(event);
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
