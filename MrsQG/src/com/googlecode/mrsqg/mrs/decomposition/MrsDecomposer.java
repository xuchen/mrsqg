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
	
	public ArrayList<MRS> decompose(ArrayList<MRS> mlist);

}
