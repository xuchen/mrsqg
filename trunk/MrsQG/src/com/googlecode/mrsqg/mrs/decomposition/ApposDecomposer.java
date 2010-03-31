/**
 * 
 */
package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.FvPair;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.mrs.Var;

/**
 * Apposition decomposer. Apposition is a relationship between 
 * two or more words in which the units are grammatically parallel 
 * and have the same referent (e.g. my friend Sue) (Concise Oxford 
 * English Dictionary).
 * 
 * This class separates the ARG1 (my friend) and ARG2 (Sue) of an 
 * APPOS_REL and generates two sentences with ARG1 and ARG2 individually 
 * (My friend is a young. Sue is young. <-= My friend Sue is young.).
 * Then the NER could work better.
 * 
 * @author Xuchen Yao
 *
 */
public class ApposDecomposer implements MrsDecomposer {
	
	private static Logger log = Logger.getLogger(ApposDecomposer.class);
	
	public ArrayList<MRS> doIt(ArrayList<MRS> inList) {
		ArrayList<MRS> outList = new ArrayList<MRS>();
		
		ArrayList<MRS> decomposedList;
		
		decomposedList = decompose(inList);
		while (decomposedList.size() != 0) {
			outList.addAll(0, decomposedList);
			decomposedList = decompose(decomposedList);
		}
		
		outList.addAll(inList);
		
		return outList;
	}


	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		
		String apposEPlabel = "APPOS_REL";
		//String[] argList = new String[] {"ARG1", "ARG2"};
		
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {

				String typeName = ep.getTypeName();

				// TODO: VERY IMPORTANT! multiple APPOS_REL!
				// see warnings in getEPbyTypeName() and getEPbyLabelValue().
				if (apposEPlabel.equals(typeName)) {
					// Bingo! Found an apposition EP! Remove it later!

					String arg1Value = ep.getValueByFeature("ARG1");
					String arg2Value = ep.getValueByFeature("ARG2");

					if (arg1Value==null || arg2Value==null) {
						log.error("Error: APPOS_REL should have ARG1 and ARG2:\n"+ep);
						continue;
					}
					ep.setFlag(true);
					MRS arg1Mrs = new MRS(mrs);
					MRS arg2Mrs = new MRS(mrs);
					ep.setFlag(false);
					
					ArrayList<ElementaryPredication> arg1EPlist = arg1Mrs.getEPbyFeatAndValue("ARG0", arg1Value);
					if (arg1EPlist.size()==0) {
						log.error("Error: can't find an EP with ARG0:"+arg1Value+" in "+arg1Mrs);
						continue;
					}
					ArrayList<ElementaryPredication> arg2EPlist = arg2Mrs.getEPbyFeatAndValue("ARG0", arg2Value);
					if (arg2EPlist.size()==0) {
						log.error("Error: can't find an EP with ARG0:"+arg2Value+" in "+arg2Mrs);
						continue;
					}
					
					// WARNING: the order of the 3 following operations are very important.
					// DO NOT CHANGE IT!
					
					// remove Appositive EP
					arg1Mrs.removeEPbyFlag();
					arg2Mrs.removeEPbyFlag();
					
					// replace all referred ARG1 to ARG2 and vice versa
					// another bug here: can't be really replaced! sigh!
					ArrayList<FvPair> arg2list = arg1Mrs.getFvPairByValue(arg2Value);
					Var arg1Var = arg1EPlist.get(0).getValueVarByFeature("ARG0");
					if (arg2list.size() != 0) {
						for (FvPair p:arg2list) {
							p.setVar(arg1Var);
						}
					}
					ArrayList<FvPair> arg1list = arg2Mrs.getFvPairByValue(arg1Value);
					Var arg2Var = arg2EPlist.get(0).getValueVarByFeature("ARG0");
					if (arg1list.size() != 0) {
						for (FvPair p:arg1list) {
							p.setVar(arg2Var);
						}
					}
					
					// remove the other ARG EP
					arg1Mrs.removeEPlist(arg1EPlist);
					arg2Mrs.removeEPlist(arg2EPlist);
					
					arg1Mrs.cleanHCONS();
					arg2Mrs.cleanHCONS();
					
					outList.add(arg1Mrs);
					outList.add(arg2Mrs);
					
					// this break is used only when this function is called
					// from the doIt(), which recursively calls decompose(). 
					break;
				}
			}
		}

		return outList;
	}

}
