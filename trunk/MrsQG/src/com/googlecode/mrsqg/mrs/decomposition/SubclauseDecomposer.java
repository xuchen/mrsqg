/**
 * Test sentences:
pipe: we live in a society which imposed no limits on what we do.
pipe: we live in a society flooded with people.
pipe: we live in a society flooded greatly.
pipe: we live in a society which sucks.
pipe: we live in a society flooded with people.
pipe: We live in an imposed society.
pipe: this is the apple eaten by the cat.
pipe: Given that our desires often conflict, it would be impossible for us to live in a society.
pipe: There are three ways in which businesses can respond to the green imperative.
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.HCONS;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * @author Xuchen Yao
 * @since 2010-04-03
 *
 */
public class SubclauseDecomposer extends MrsDecomposer {

	private static Logger log = Logger.getLogger(SubclauseDecomposer.class);

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	@Override
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {

		if (inList == null) return null;
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS inMrs:inList) {

			ElementaryPredication verbEP = null;
			String oneArg = null;

			for (ElementaryPredication ep:inMrs.getEps()) {
				MRS mrs = null;
				String oriTense = null;
				// find out all verb EPs who are not the main verb of the sentence
				// not main verb: ARG0 value isn't the index of this mrs
				if (ep.isVerbEP() && !ep.getArg0().equals(inMrs.getIndex()) &&
						ep.getValueVarByFeature("ARG0").getExtrapair().get("SF").startsWith("PROP")) {

					mrs = new MRS(inMrs);
					oriTense = mrs.getTense();
					// remove all verb EPs which don't have an ARG* in front of it
					// e.g. "a generated sentence" (doesn't really work)
					HashSet<String> argSet = ep.getAllARGvalue();

					for (ElementaryPredication e:inMrs.getEps()) {
						// don't loop after ep
						if (e==ep) break;
						if (argSet.contains(e.getArg0())) {
							verbEP = mrs.getEps().get(inMrs.getEps().indexOf(ep));
							oneArg = e.getArg0();
							break;
						}
					}

					if (verbEP == null) continue;
					if (mrs == null) continue;

					mrs.keepDependentEPfromVerbEP(verbEP);

					/*
					 *  set the lowEP of oneArg (verbEP's ARG1, usually before verbEP) to a different label
					 */
					ArrayList<ElementaryPredication> argList = mrs.getEPbyFeatAndValue("ARG0", oneArg);
					if (argList == null) continue;
					if (argList.size() == 1 && argList.get(0).getLabel().equals(verbEP.getLabel())) {
						argList.get(0).setLabel("h"+mrs.generateUnusedLabel(1).get(0));
					} else if (argList.size() == 2) {
						int hiEPidx = MRS.determineHiEPindex(argList, mrs);
						ElementaryPredication hiEP = argList.get(hiEPidx);
						ElementaryPredication lowEP = argList.get(1-hiEPidx);
						if (lowEP.getLabel().equals(verbEP.getLabel())) {
							String oldLowLabel = lowEP.getLabel();
							// correct the HCONS list
							String newLowLabel = "h"+mrs.generateUnusedLabel(1).get(0);
							lowEP.setLabel(newLowLabel);
							for (ElementaryPredication cEP:lowEP.getAllConnections()) {
								if (cEP!=verbEP && cEP.getLabel().equals(oldLowLabel)) {
									/*
									 * This is a pretty fish eaten by the cat.
									 * "pretty" and "eaten" have the same label with "fish",
									 * we must also change "pretty"'s label, but not "eaten"'s label
									 */
									cEP.setLabel(newLowLabel);
								}
							}
							for (HCONS h:mrs.getHcons()) {
								if (h.getHi().equals(hiEP.getValueByFeature("RSTR")) && h.getLo().equals(oldLowLabel)) {
									h.getLoVar().setLabel(newLowLabel);
									break;
								}
							}
						}
					} else {
						log.error("the size of one arg list of the subclause verb isn't 1 or 2:\n"+argList);
					}

					mrs.setIndex(verbEP.getArg0());
					if (mrs.getTense().equals("UNTENSED"))
						mrs.setTense(oriTense);
					if (mrs.removeEPbyFlag()) {
						mrs.setDecomposer("Subclause");
						mrs.cleanHCONS();
						outList.add(mrs);
					}

				}


			}
		}

		return outList.size() == 0 ? null : outList;
	}

}
