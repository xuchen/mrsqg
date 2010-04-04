/**
 * 
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
			MRS mrs = new MRS(inMrs);
 
			ElementaryPredication verbEP = null;
			ElementaryPredication oneArgEP = null;
			String oneArg = null;
			String otherArg = null;
			String oriTense = mrs.getTense();
			
			for (ElementaryPredication ep:mrs.getEps()) {

				// find out all verb EPs who are not the main verb of the sentence
				// verb EP: type name matches _v_ and ARG0 is an event (with SF:PROP)
				// not main verb: ARG0 value isn't the index of this mrs
				if (ep.getTypeName().contains("_v_") && 
						ep.getArg0().startsWith("e") && 
						!ep.getArg0().equals(mrs.getIndex()) &&
						ep.getValueVarByFeature("ARG0").getExtrapair().get("SF").startsWith("PROP")) {
					
					// remove all verb EPs which don't have an ARG* in front of it
					// e.g. "a generated sentence" (doesn't really work)
					HashSet<String> argSet = ep.getAllARGvalue();

					for (ElementaryPredication e:mrs.getEps()) {
						// don't loop after ep
						if (e==ep) break;
						if (argSet.contains(e.getArg0())) {
							verbEP = ep;
							oneArg = e.getArg0();
						} else {
							// every EP before verbEP who isn't an argument of
							// verbEP is set to true (for removal later).
							e.setFlag(true);
						}
					}
					if (verbEP != null) break;
				}
			}
			
			if (verbEP == null) continue;
			for (String arg:verbEP.getAllARGvalue()) {
				if (arg.startsWith("e")) continue;
				if (arg.equals(oneArg)) continue;
				else {otherArg = arg; break;}
			}
			
			// otherArg doesn't really exist
			if (otherArg != null && mrs.getEPbyFeatAndValue("ARG0", otherArg).size() == 0) {
				otherArg = null;
			}
			

			// the initial set contains otherArg and the label of verbEP.
			// For all other EPs after verbEP, if they do not refer to the
			// initial set, then mark them for deletion. otherwise, add
			// them into the initial set and move on to the next one.
			HashSet<String> subSet = new HashSet<String>();
			if (otherArg != null) subSet.add(otherArg);
			subSet.add(verbEP.getLabel());
			int i = 2;
			while (i-->0) {
				// we should have done it recursively, but for the
				// efficiency of laziness, we do it only twice.;-)
				for (ElementaryPredication ep:mrs.getEps()) {
					if (ep.getCto() < verbEP.getCto()) continue;
					if (ep == verbEP) continue;
					HashSet<String> epSet = ep.getAllValue();
					epSet.add(ep.getLabel());
					boolean flag = true;
					for (String s:epSet) {
						if (subSet.contains(s)) {
							subSet.addAll(epSet);
							flag = false;
							break;
						}
					}
					ep.setFlag(flag);
				}
			}

			
			// set the lowEP of oneArg to a different label
			ArrayList<ElementaryPredication> argList = mrs.getEPbyFeatAndValue("ARG0", oneArg);
			if (argList.size() == 1) {
				argList.get(0).setLabel("h"+mrs.generateUnusedLabel(1).get(0));
			} else if (argList.size() == 2) {
				int hiEPidx = MRS.determineHiEPindex(argList, mrs);
				ElementaryPredication hiEP = argList.get(hiEPidx);
				ElementaryPredication lowEP = argList.get(1-hiEPidx);
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
			} else {
				log.error("the size of one arg list of the subclause verb isn't 1 or 2:\n"+argList);
			}
			
			mrs.setIndex(verbEP.getArg0());
			if (mrs.getTense().equals("UNTENSED"))
					mrs.setTense(oriTense);
			mrs.removeEPbyFlag();
			mrs.cleanHCONS();
			outList.add(mrs);
		}

		return outList.size() == 0 ? null : outList;
	}

}
