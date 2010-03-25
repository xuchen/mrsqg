/**
 * 
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 *
 */
public class CoordDecomposer implements MrsDecomposer {
	
	private static Logger log = Logger.getLogger(CoordDecomposer.class);

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		
		ArrayList<MRS> outList = new ArrayList<MRS>();
		// TODO: recursive case (multiple coordinations)
		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				String pred = ep.getPred();
				if (pred==null) pred = ep.getSpred();
				if (pred.equalsIgnoreCase("_AND_C_REL") ) {
					String lHndl = ep.getVarLabel("L-HNDL");
					String rHndl = ep.getVarLabel("R-HNDL");
					if (lHndl != null && rHndl != null) {
						// use L-HNDL and R-HNDL to assemble two individual MRS
						String loLabel = null;
						String targetLabel = null;
						// get the loLabel from the HCONS list
						for (String hiLabel: new String[]{lHndl, rHndl}) {
							loLabel = MRS.getLoLabelFromHconsList(hiLabel, mrs.getHcons());
							// targetLabel indicates the EP we want to extract
							if (loLabel != null)
								targetLabel = loLabel;
							else {
								targetLabel = hiLabel;
								log.warn("In this MRS, the HNDL of a coordination is not" +
										" in a qeq relation. Using HiLabel as the target for extraction.");
							}
							outList.add(MRS.extractByLabel(targetLabel, mrs));
						}
					}
				}
			}
		}

		return outList;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
