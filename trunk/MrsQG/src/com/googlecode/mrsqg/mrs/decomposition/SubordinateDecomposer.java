/**
pipe: Given that our desires often conflict, it would be impossible for us to live in a society.
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.EP;
import com.googlecode.mrsqg.mrs.FvPair;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 *
 */
public class SubordinateDecomposer extends MrsDecomposer {

	private static Logger log = Logger.getLogger(SubordinateDecomposer.class);

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	@Override
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {

		if (inList == null) return null;
		/*
		 * TODO: Some _x_*rel can also indicate subordinate relation,
		 * such as  _even+though_x_rel, _if_x_then_rel.
		 * But some _x_*rel has only one argument, such as _largely_x_rel,
		 * so they should be ruled out. Refer to core.smi to dig all out.
		 */
		String subord = "SUBORD_REL";
		String[] args = {"ARG1", "ARG2"};
		String label;

		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (EP ep:mrs.getEps()) {
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
						EP subEP = subMrs.getEps().get(mrs.getEps().indexOf(ep));

						//subMrs.keepDependentEPbyLabel(label, subEP);
						subMrs.doDecompositionByLabel(label, subEP, true, true);

						//set the index for this EP, have to be verbEP's index
						EP vEP;
						if (label.startsWith("h")) {
							String loLabel = subMrs.getLoLabelFromHconsList(label);
							if (loLabel != null) label = loLabel;
							vEP = MRS.getDependentEP(subMrs.getEPbyLabelValue(label));
						} else {
							vEP = subMrs.getCharVariableMap().get(label);
						}
						if (vEP==null) {
							log.error("Error: can't find a verb EP from MRS:");
							log.error(subMrs);
							log.error("Debug your code!");
						} else {
							subMrs.setIndex(vEP.getArg0());
						}

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
