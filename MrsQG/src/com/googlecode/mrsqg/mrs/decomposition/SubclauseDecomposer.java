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
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.EP;
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

			EP tEP = null;

			for (EP ep:inMrs.getEps()) {
				MRS mrs = null;
				String oriTense = null;
				// find out all verb EPs who are not the main verb of the sentence
				// not main verb: ARG0 value isn't the index of this mrs

				if ((ep.isVerbEP() || ep.isPrepositionEP())&& !ep.getArg0().equals(inMrs.getIndex()) &&
						ep.getValueVarByFeature("ARG0").getExtrapair().get("SF").startsWith("PROP") &&
						!ep.hasEPemptyArgsExceptPassive() && ep.hasEQarg()) {

					if (!ep.hasEQargToNonPPorVerb()) {
						/*
						 * In the case of "Mary is the girl John fell in love with"
						 * we don't want to go through 'fell' and 'in'
						 */
						continue;
					}

					mrs = new MRS(inMrs);
					oriTense = mrs.getTense();
					tEP = mrs.getEps().get(inMrs.getEps().indexOf(ep));

					//mrs.keepDependentEPfromVerbEP(verbEP);
					mrs.doDecomposition(new HashSet<EP>(Arrays.asList(tEP)), null, true, true);

					/*
					 *  set the lowEP of oneArg (verbEP's ARG1, usually before verbEP) to a different label
					 */
//					ArrayList<EP> argList = mrs.getEPbyFeatAndValue("ARG0", oneArg);
//					if (argList == null) continue;
//					if (argList.size() == 1 && argList.get(0).getLabel().equals(verbEP.getLabel())) {
//						argList.get(0).setLabel("h"+mrs.generateUnusedLabel(1).get(0));
//					} else if (argList.size() == 2) {
//						ArrayList<EP> hiloEPS = MRS.determineHiLowEP (argList, mrs);
//						if (hiloEPS == null) continue;
//						EP hiEP = hiloEPS.get(0);
//						EP lowEP = hiloEPS.get(1);
//						if (lowEP.getLabel().equals(verbEP.getLabel())) {
//							String oldLowLabel = lowEP.getLabel();
//							// correct the HCONS list
//							String newLowLabel = "h"+mrs.generateUnusedLabel(1).get(0);
//							lowEP.setLabel(newLowLabel);
//							for (EP cEP:lowEP.getAllConnections()) {
//								if (cEP!=verbEP && cEP.getLabel().equals(oldLowLabel)
//										&& !cEP.getTypeName().contains("_D_")) {
//									/*
//									 * This is a pretty fish eaten by the cat.
//									 * "pretty" and "eaten" have the same label with "fish",
//									 * we must also change "pretty"'s label, but not "eaten"'s label
//									 */
//									cEP.setLabel(newLowLabel);
//								}
//							}
//							for (HCONS h:mrs.getHcons()) {
//								if (h.getHi().equals(hiEP.getValueByFeature("RSTR")) && h.getLo().equals(oldLowLabel)) {
//									h.getLoVar().setLabel(newLowLabel);
//									break;
//								}
//							}
//						}
//					} else {
//						log.error("the size of one arg list of the subclause verb isn't 1 or 2:\n"+argList);
//					}

					if (tEP.isVerbEP()) {
						mrs.setIndex(tEP.getArg0());
					} else {
						// is preposition
						EP vEP = EP.getVerbEP(tEP);
						if (vEP != null)
							mrs.setIndex(vEP.getArg0());
						else
							continue;
					}
					if (mrs.getTense().equals("UNTENSED"))
						mrs.setTense(oriTense);
					mrs.setDecomposer("Subclause");
					if (mrs.removeEPbyFlag(true)) {
						mrs.cleanHCONS();
						outList.add(mrs);
					}

				}


			}
		}

		return outList.size() == 0 ? null : outList;
	}

}
