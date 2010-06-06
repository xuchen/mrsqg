/**
pipe: Given that our desires often conflict, it would be impossible for us to live in a society.
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.FvPair;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 *
 */
public class SubordinateDecomposer extends MrsDecomposer {

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	@Override
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {

		if (inList == null) return null;
		String subord = "SUBORD_REL";
		String[] args = {"ARG1", "ARG2"};
		String label;

		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().toUpperCase().contains(subord)) {
					// loop through ARG1, ARG2
					for (String arg:args) {
						label = null;
						for (FvPair p:ep.getFvpair()) {
							if (p.getFeature().equals(arg)) {
								label = p.getValue();
								break;
							}
						}
						if (label==null) continue;

						MRS subMrs = new MRS(mrs);
						ElementaryPredication subEP = subMrs.getEps().get(mrs.getEps().indexOf(ep));

						subMrs.keepDependentEP(label, subEP);
						if (subMrs.removeEPbyFlag()) {
							subMrs.cleanHCONS();
							subMrs.setDecomposer("Subordinate");
							outList.add(subMrs);
						}
					}
				}
			}
		}
		return outList.size() == 0 ? null : outList;
	}

}
