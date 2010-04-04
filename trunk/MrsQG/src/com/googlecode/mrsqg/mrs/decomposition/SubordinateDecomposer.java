/**
 * 
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
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
		
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				if (ep.getTypeName().equals(subord)) {
					// only copy when found, for efficiency reasons.
					MRS subMrs = new MRS(mrs);
					ElementaryPredication subEP = subMrs.getEps().get(mrs.getEps().indexOf(ep));
					
					/* in the most simple case, subEP's range covers the whole
					 * subordinate clause, so remove all.
					 */
					for (ElementaryPredication e:subMrs.getEps()) {
						if (e.getCto() > subEP.getCto()) break;
						e.setFlag(true);
					}
					subMrs.removeEPbyFlag();
					subMrs.cleanHCONS();
					outList.add(subMrs);
					break;
				}
			}
		}
		return outList.size() == 0 ? null : outList;
	}

}
