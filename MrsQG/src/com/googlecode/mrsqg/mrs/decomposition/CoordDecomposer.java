package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		String[] coordEP = new String[]{"_AND_C_REL", "_OR_C_REL", "_BUT_C_REL"};
		List<String> coordEPlist = Arrays.asList(coordEP);
		
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				String pred = ep.getPred();
				if (pred==null) pred = ep.getSpred();
				if (coordEPlist.contains(pred)) {
					String lHndl = ep.getValueByFeature("L-HNDL");
					String rHndl = ep.getValueByFeature("R-HNDL");
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
							outList.add(MRS.extractByLabelValue(targetLabel, mrs));
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
