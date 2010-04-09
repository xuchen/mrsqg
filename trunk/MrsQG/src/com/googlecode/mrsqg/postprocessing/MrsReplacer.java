/**
 * Current this class replaces a coordination phrase (indicated by the
 * _AND_C_REL relation) with "what", and tries to generate from it.
 */
package com.googlecode.mrsqg.postprocessing;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.mrsqg.Preprocessor;
import com.googlecode.mrsqg.analysis.Pair;
import com.googlecode.mrsqg.mrs.ElementaryPredication;
import com.googlecode.mrsqg.mrs.MRS;
import com.googlecode.mrsqg.nlp.Cheap;
import com.googlecode.mrsqg.nlp.LKB;

/**
 * @author Xuchen Yao
 * @since 2010-04-05
 */
public abstract class MrsReplacer {
	
	protected static Logger log = Logger.getLogger(MrsReplacer.class);
	protected Cheap parser;
	protected LKB generator;
	protected Preprocessor pre;
	protected ArrayList<MRS> origList;
	
	protected ArrayList<Pair> pairs;
	
	public MrsReplacer (Cheap cheap, LKB lkb, Preprocessor pre, ArrayList<MRS> list) {
		this.parser = cheap;
		this.generator = lkb;
		this.pre = pre;
		this.origList = list;
		this.pairs = new ArrayList<Pair>();
	}
	

	public abstract void doIt ();
	
	protected void genFromParse () {
		Preprocessor pre;
		for (Pair p:this.pairs) {
			pre = new Preprocessor();
			String fsc = pre.getFSCbyTerms(p.getTranSent(), true);
			log.info("Transformed sentence:");
			log.info(p.getTranSent());
//			log.info("\nFSC XML from preprocessing:\n");
//			log.info(fsc);
			parser.parse(fsc);
			ArrayList<MRS> mrxList = parser.getParsedMRSlist();
			p.setTranMrs(mrxList);
			if (pre.getNumTokens() > 15) {
				parser.releaseMemory();
			}
			if (!parser.isSuccess()) continue;
			// TODO: add MRS selection here

			if (mrxList != null && this.generator != null) {
				String mrx;
				for (MRS m:mrxList) {
					// generate from original sentence
					m.changeFromUnkToNamed();
					mrx = m.toMRXstring();
					generator.sendMrxToGen(mrx);
					log.info("\nGenerate from transformed sentence:\n");
					ArrayList<String> genSents = generator.getGenSentences();
					log.info(genSents);
//					log.info("\nFrom the following MRS:\n");
//					log.info(mrx);
//					log.info(m);
					if (genSents != null)
						p.getGenSentList().addAll(genSents);
				}
			}
		}
	}

}
