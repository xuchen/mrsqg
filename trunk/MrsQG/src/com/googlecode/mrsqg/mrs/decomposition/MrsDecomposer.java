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
public interface MrsDecomposer {
	
	/**
	 * Given a list of MRS, decompose it into simpler ones
	 * @param inList the original list of MRS to be decomposed.
	 * @return a new ArrayList of decomposed MRS, without the original ones.
	 */
	public ArrayList<MRS> decompose(ArrayList<MRS> inList);

}
