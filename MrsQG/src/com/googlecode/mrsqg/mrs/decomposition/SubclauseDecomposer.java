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
			String otherArg = null;

			for (ElementaryPredication ep:inMrs.getEps()) {
				MRS mrs = null;
				String oriTense = null;
				// find out all verb EPs who are not the main verb of the sentence
				// verb EP: type name matches _v_ and ARG0 is an event (with SF:PROP)
				// not main verb: ARG0 value isn't the index of this mrs
				if (ep.getTypeName().contains("_v_") && 
						ep.getArg0().startsWith("e") && 
						!ep.getArg0().equals(inMrs.getIndex()) &&
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
				}


				// find out the other arg
				if (verbEP == null) continue;
				if (mrs == null) continue;
				for (String arg:verbEP.getAllARGvalue()) {
					if (arg.startsWith("e")) continue;
					if (arg.equals(oneArg)) continue;
					else {otherArg = arg; break;}
				}

				// otherArg doesn't really exist, this verb is intransitive
				if (otherArg != null && mrs.getEPbyFeatAndValue("ARG0", otherArg) == null) {
					otherArg = null;
				}


				// the initial set contains otherArg and the label of verbEP.
				// For all other EPs after verbEP, if they do not refer to the
				// initial set, then mark them for deletion. otherwise, add
				// them into the initial set and move on to the next one.
				HashSet<String> subSet = new HashSet<String>();
				if (otherArg != null) subSet.add(otherArg);
				subSet.add(oneArg);
				subSet.add(verbEP.getLabel());
				int i = 2;
				while (i-->0) {
					// we should have done it recursively, but for the
					// efficiency of laziness, we do it only twice.;-)
					ArrayList<ElementaryPredication> noDelList = new ArrayList<ElementaryPredication>();
					for (ElementaryPredication eep:mrs.getEps()) {
						if (noDelList.contains(eep)) {
							eep.setFlag(false);
							continue;
						}
						HashSet<String> epSet;
						//if (ep.getCto() < verbEP.getCto()) continue;
						if (eep.getCto() < verbEP.getCto()) {
							if (subSet.contains(eep.getArg0())) {
								eep.setFlag(false);
								// don't delete every EP with the same range of ep
								noDelList.addAll(mrs.getEPS(eep.getCfrom(), eep.getCto()));
							} else eep.setFlag(true);
							continue;
						}
						if (eep == verbEP) continue;
						if ((eep.getTypeName().contains("_v_") || eep.getTypeName().contains("_V_")) && 
								eep.getCfrom() < verbEP.getCfrom()) {
							eep.setFlag(true);
							continue;
						}
						epSet = eep.getAllValue();
						epSet.add(eep.getLabel());
						boolean flag = true;
						for (String s:epSet) {
							if (subSet.contains(s)) {
								subSet.addAll(epSet);
								String lo = mrs.getLoLabelFromHconsList(s);
								if (lo != null) subSet.add(lo);
								flag = false;
								break;
							}
						}
						eep.setFlag(flag);
					}
				}


				// set the lowEP of oneArg to a different label
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

		return outList.size() == 0 ? null : outList;
	}

}
