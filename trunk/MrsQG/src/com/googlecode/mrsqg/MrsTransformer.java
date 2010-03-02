package com.googlecode.mrsqg;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;

/**
 * This class transforms the MRS of a declarative sentence 
 * into that of a interrogative sentence. 
 * 
 * @author Xuchen Yao
 * @version 2010-03-02
 */
public class MrsTransformer {
	
	private static Logger log = Logger.getLogger(MrsTransformer.class);
	
	private Preprocessor pre;
	
	/**
	 * MRS for the original sentence
	 */
	private MRS ori_mrs;
	
	/**
	 * a list of MRS for the generated questions. 
	 * Each question is represented by one MRS 
	 */
	private ArrayList<MRS> gen_mrs;
	
	public MrsTransformer (String file, Preprocessor p) {
		this.ori_mrs = new MRS(file);
		this.pre = p;
		this.gen_mrs = new ArrayList<MRS>();
	}
	
	public void transform () {
		Term[] terms = pre.getTerms()[0];
		if (terms == null) return;
		
		String neType;
		ArrayList<ElementaryPredication> eps;
		
		for (Term term:terms) {
			neType = Arrays.toString(term.getNeTypes());
			if (neType.length()==0) {
				log.error("NE types shouldn't be none: "+term);
			}
			MRS q_mrs = new MRS(ori_mrs);
			if (neType.contains("NEperson")) {
				eps = this.ori_mrs.getEPS(term.getFrom(), term.getTo());
			}
		}
	}

	public static void main(String[] args) {

	}

}
