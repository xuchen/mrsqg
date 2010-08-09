/**
 * An MRS selector for PET's output. Any parsed MRS that can't
 * generate through LKB is filtered out.
 */
package com.googlecode.mrsqg.mrs.selection;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.LKB;
import com.googlecode.mrsqg.postprocessing.MrsReplacer;

/**
 * @author Xuchen Yao
 *
 */
public class PreSelector {
	protected static Logger log = Logger.getLogger(PreSelector.class);
	
	public static ArrayList<MRS> doIt (LKB lkb, ArrayList<MRS> origMrsList) {
		if (lkb == null) return origMrsList;
		if (origMrsList == null) return null;
		ArrayList<MRS> list = new ArrayList<MRS>();
		
		log.info("Entering PreSelector... Original MRS list size: "+origMrsList.size());
		String mrx;
		for (MRS m:origMrsList) {
			// generate from original sentence
			m.changeFromUnkToNamed();
			mrx = m.toMRXstring();
			lkb.sendMrxToGen(mrx);
			ArrayList<String> genSents = lkb.getGenSentences();
			if (genSents != null) list.add(m);
		}
		
		log.info("Exiting PreSelector... Selected MRS list size: "+list.size());
		return list.size()==0 ? null : list;
	}

}
