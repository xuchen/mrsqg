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
public class ApposDecomposer extends MrsDecomposer {
	
	private static Logger log = Logger.getLogger(ApposDecomposer.class);
	
	private String apposEPlabel = "APPOS_REL";

	/* (non-Javadoc)
	 * @see com.googlecode.mrsqg.mrs.decomposition.MrsDecomposer#decompose(java.util.ArrayList)
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList) {
		
		//String[] argList = new String[] {"ARG1", "ARG2"};
		
		ArrayList<MRS> outList = new ArrayList<MRS>();

		for (MRS mrs:inList) {
			for (ElementaryPredication ep:mrs.getEps()) {

				String typeName = ep.getTypeName();

				// TODO: VERY IMPORTANT! multiple APPOS_REL!
				// see warnings in getEPbyTypeName() and getEPbyLabelValue().
				if (apposEPlabel.equals(typeName)) {
					// Bingo! Found an apposition EP!
					
					// PART I: use this apposition to form a short sentence
					// for instance, "the girl Anna" -> "the girl is Anna."
					outList.add(assembleApposition2sent(mrs, ep));
					
					// PART II: decompose the sentence by the first apposition.
					// for instance, "the girl Anna likes dogs."
					// -> "the girl like dogs." && "Anna likes dogs."
					outList.addAll(divideApposition2sent(mrs, ep));
					
					// this break is used only when this function is called
					// from the doIt(), which recursively calls decompose(). 
					break;
				}
			}
		}

		return outList;
	}
	
	/**
	 * Use the apposition EP <code>apposEP</code> in <code>mrs</code> to 
	 * form a short sentence. For instance, "the girl Anna" -> "the girl is Anna."
	 * @param mrs an input MRS
	 * @param apposEP the apposition EP, must be in <code>mrs</code>
	 * @return a new MRS representing a simple sentence formed by apposition
	 */
	public MRS assembleApposition2sent (MRS mrs, ElementaryPredication apposEP) {
		
		MRS apposMrs = MRS.extractByEPandArg0(apposEP, mrs);
		
		// It should contain only 1 EP. We have 3 steps here:
		// 1. change APPOS_REL to _BE_V_ID_REL
		// 2. change the main event of sentence to ARG0 of _BE_V_ID_REL
		// 3. change TENSE of the event to PRES
		// 3. change SF to QUES

		ElementaryPredication newApposEP = apposMrs.getEPbyTypeName(apposEPlabel).get(0);
		// step 1
		newApposEP.setTypeName("_BE_V_ID_REL");
		// step 2
		apposMrs.setIndex(newApposEP.getArg0());
		// step 3
		newApposEP.getValueVarByFeature("ARG0").setExtrapairValue("TENSE", "PRES");
		// step 4
		apposMrs.setAllSF2QUES();
		
		return apposMrs;
	}
	
	/**
	 * Decompose the sentence by the first apposition EP <code>ep</code>.
	 * for instance, "the girl Anna likes dogs."
	 * -> "the girl like dogs." && "Anna likes dogs."
	 * @param mrs an input MRS
	 * @param ep the apposition EP, must be in <code>mrs</code>
	 * @return a new list of MRS representing a simple sentence formed by apposition
	 */
	public ArrayList<MRS> divideApposition2sent (MRS mrs, ElementaryPredication ep) {

		ArrayList<MRS> outList = new ArrayList<MRS>();

		String arg1Value = ep.getValueByFeature("ARG1");
		String arg2Value = ep.getValueByFeature("ARG2");

		if (arg1Value==null || arg2Value==null) {
			log.error("Error: APPOS_REL should have ARG1 and ARG2:\n"+ep);
			return null;
		}
		ep.setFlag(true);
		MRS arg1Mrs = new MRS(mrs);
		MRS arg2Mrs = new MRS(mrs);
		ep.setFlag(false);
		
		ArrayList<ElementaryPredication> arg1EPlist = arg1Mrs.getEPbyFeatAndValue("ARG0", arg1Value);
		if (arg1EPlist.size()==0) {
			log.error("Error: can't find an EP with ARG0:"+arg1Value+" in "+arg1Mrs);
			return null;
		}
		ArrayList<ElementaryPredication> arg2EPlist = arg2Mrs.getEPbyFeatAndValue("ARG0", arg2Value);
		if (arg2EPlist.size()==0) {
			log.error("Error: can't find an EP with ARG0:"+arg2Value+" in "+arg2Mrs);
			return null;
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
		
		return outList;
	}

}
