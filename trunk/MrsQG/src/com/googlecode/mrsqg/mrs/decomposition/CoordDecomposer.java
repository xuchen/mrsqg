package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
		if (inList == null) return null;
		
		String[] coordEPnames = new String[]{"_AND_C_REL", "_OR_C_REL", "_BUT_C_REL"/*, "IMPLICIT_CONJ_REL"*/};
		List<String> coordEPlist = Arrays.asList(coordEPnames);
		
		ArrayList<MRS> outList = new ArrayList<MRS>();
		ElementaryPredication coordEP = null;

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {
				String pred = ep.getPred();
				if (pred==null) pred = ep.getSpred();
				if (coordEPlist.contains(pred)) {
					String lHndl = ep.getValueByFeature("L-HNDL");
					String lEvent = ep.getValueByFeature("L-INDEX");
					String rHndl = ep.getValueByFeature("R-HNDL");
					String rEvent = ep.getValueByFeature("R-INDEX");
					String llowLabel = mrs.getLoLabelFromHconsList(lHndl);
					String rlowLabel = mrs.getLoLabelFromHconsList(rHndl);
					if (lHndl != null && rHndl != null && 
							(mrs.getEPbyLabelValue(lHndl) != null || (llowLabel!=null && mrs.getEPbyLabelValue(llowLabel) != null) )&& 
							(mrs.getEPbyLabelValue(rHndl) != null || (rlowLabel!=null && mrs.getEPbyLabelValue(rlowLabel) != null))) {
						// use L-HNDL and R-HNDL to assemble two individual MRS
						String loLabel = null;
						String targetLabel = null;
						// get the loLabel from the HCONS list
						for (String hiLabel: new String[]{lHndl, rHndl}) {
							coordEP = ep;
							loLabel = mrs.getLoLabelFromHconsList(hiLabel);
							// targetLabel indicates the EP we want to extract
							if (loLabel != null)
								targetLabel = loLabel;
							else {
								targetLabel = hiLabel;
							}
							//outList.add(MRS.extractByLabelValue(targetLabel, mrs));
							MRS cMrs = new MRS(mrs);
							coordEP = cMrs.getEps().get(mrs.getEps().indexOf(coordEP));
							coordEP.setFlag(true);
							
							HashSet<String> subSet = new HashSet<String>();
							subSet.add(targetLabel);
							for (ElementaryPredication ee:cMrs.getEPbyLabelValue(targetLabel)) {
								subSet.addAll(ee.getAllValue());
								subSet.add(ee.getLabel());
							}
							
							/*
							 * In cases "John likes Mary and hates Anna.",
							 * we want to keep "John" and "hates Anna", while
							 * "John"'s Arg0 is referred by a value in the subSet from "hates".
							 */
							if (hiLabel.equals(rHndl)) {
								for (ElementaryPredication e:cMrs.getEps()) {
									if (e == coordEP) break;
									if (!subSet.contains(e.getArg0())) {
										e.setFlag(true);
									}
								}
							}
							
							int i = 2;
							while (i-->0) {
								// we should have done it recursively, but for the
								// efficiency of laziness, we do it only twice.;-)
								for (ElementaryPredication e:cMrs.getEps()) {
									// warning: the following order is important, don't change it!
									if (e == coordEP) {
										continue;
									}
									if (e.getFlag()) continue;
									if (hiLabel.equals(rHndl) && e.getCto()<coordEP.getCto()) {
										continue;
									}
									if (hiLabel.equals(lHndl) && e.getCto()>coordEP.getCto()) {
										e.setFlag(true);
										continue;
									}
									HashSet<String> epSet = e.getAllValue();
									epSet.add(e.getLabel());
									boolean flag = true;
									for (String s:epSet) {
										if (subSet.contains(s)) {
											subSet.addAll(epSet);
											flag = false;
											break;
										}
									}
									e.setFlag(flag);
								}
							}
							
							if (hiLabel.equals(lHndl) ) {
								cMrs.setIndex(lEvent);
							} else {
								cMrs.setIndex(rEvent);
							}
							if (cMrs.removeEPbyFlag()) {
								cMrs.cleanHCONS();
								cMrs.changeFromUnkToNamed();
								cMrs.setDecomposer("Coordination");
								outList.add(cMrs);
							}
						}
						break;
					}
//					else if (lHndl == null && rHndl == null && 
//							lEvent != null && rEvent != null) {
//						int cfrom, cto, andcfrom, andcto;
//						int[] range = mrs.extractRangeByXValue(lEvent, ep);
//						cfrom = range[0];
//						cto = range[1];
//						andcfrom = ep.getCfrom();
//						andcto = ep.getCto();
//						// "John likes green apples and red beans."
//						// -> "John likes green apples"
//						
//						// "John likes green apples and red beans."
//						// -> "John likes red beans."
//						
//					}
				}
			}
		}

		return outList.size() == 0 ? null : outList;
	}


}
