package com.googlecode.mrsqg.mrs.decomposition;

import java.util.ArrayList;

import com.googlecode.mrsqg.mrs.MRS;

/**
 * This interface uses the decompose() method to decompose complex sentences into
 * simple ones.
 * 
 * @author Xuchen Yao
 *
 */
public abstract class MrsDecomposer {
	
	
	/**
	 * Recursively decompose a list of MRS into a new one
	 * @param inList the input MRS list
	 * @return a new decomposed MRS, including the original one <code>inList</code>
	 */
	public ArrayList<MRS> doIt(ArrayList<MRS> inList) {
		ArrayList<MRS> outList = new ArrayList<MRS>();
		
		ArrayList<MRS> decomposedList;
		
		decomposedList = decompose(inList);
		while (decomposedList != null) {
			outList.addAll(0, decomposedList);
			decomposedList = decompose(decomposedList);
		}
		
		outList.addAll(inList);
		
		return outList;
	}
	
	/**
	 * Given a list of MRS, decompose it into simpler ones
	 * @param inList the original list of MRS to be decomposed.
	 * @return a new ArrayList of decomposed MRS, without the original ones.
	 */
	public abstract ArrayList<MRS> decompose(ArrayList<MRS> inList) ;

}
