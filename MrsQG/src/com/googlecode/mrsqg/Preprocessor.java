package com.googlecode.mrsqg;



import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.googlecode.mrsqg.analysis.Term;
import com.googlecode.mrsqg.analysis.TermExtractor;
import com.googlecode.mrsqg.nlp.NETagger;
import com.googlecode.mrsqg.nlp.OpenNLP;
import com.googlecode.mrsqg.util.Dictionary;
import com.googlecode.mrsqg.util.StringUtils;



/**
 * This class preprocesses a declarative sentence.
 * 
 * @author Xuchen Yao
 * @version 2010-02-26
 */
public class Preprocessor {
	
	private static Logger log = Logger.getLogger(Preprocessor.class);
	
	private int countOfSents = 0;
	private String[] originalSentences;
	private String[][] tokens;
	private String[][] pos;
	private String[] sentences;
	private String[][][] nes;
	//private ArrayList<String>[] to;
	private Term[][] terms;
	//private boolean[] firstCapitalize;
	/** <code>Dictionaries</code> for term extraction. */
	private static ArrayList<Dictionary> dicts = new ArrayList<Dictionary>();
	
	String[] getOriginalSentences() {return this.originalSentences;}
	String[][] getTokens() {return this.tokens;}
	String[] getSentences() {return this.sentences;}
	Term[][] getTerms () {return this.terms;}
	
	public boolean preprocess (String sents) {
		log.debug("Preprocessing");
		
		String[] originalSentences = OpenNLP.sentDetect(sents);
		this.countOfSents = originalSentences.length;
		log.debug("Count of original one: "+countOfSents);
		
		String original;
		this.tokens = new String[countOfSents][];
		this.pos = new String[countOfSents][];
		this.sentences = new String[countOfSents];
		for (int i = 0; i < countOfSents; i++) {
			original = originalSentences[i];
			log.debug("Sentence "+i+" :"+original);
			tokens[i] = NETagger.tokenize(original);
			pos[i] = OpenNLP.tagPos(tokens[i]);
			sentences[i] = StringUtils.concatWithSpaces(this.tokens[i]);
		}
		
		this.terms = new Term[this.countOfSents][];
		// extract named entities
		this.nes = NETagger.extractNes(this.tokens);
		if (this.nes != null) {
			for (int i=0; i<this.countOfSents; i++){
				original = originalSentences[i];
				this.terms[i] = TermExtractor.getTerms(original, "", this.nes[i],
						Preprocessor.getDictionaries());
				log.debug("Sentence "+i+" terms:");
				for (int j=0; j<this.terms[i].length; j++) {
					log.debug(this.terms[i][j]+"  ");
				}
			}
		}
		
		return true;
	}
	
	/**
	 * return the preposition before a term t in sentence number sentNum, if any
	 * @param t the term, such as "Germany"
	 * @param sentNum the sentence number
	 * @return the preposition, such as "in"
	 */
	public String getPrepositionBeforeTerm (Term t, int sentNum) {
		String p = null;
		String pPos = pos[sentNum][t.getFrom()-1];
		if (pPos.equalsIgnoreCase("IN")) {
			p = tokens[sentNum][t.getFrom()-1];
		}
		return p;
	}
	
	/**
	 * Output preprocessed sentence to FSC format by tokens
	 * 
	 * <p>This is the <tt>fsc.dtd</tt>: </p>
	 * 
	 * <pre>
	 * &lt;!ELEMENT fsc ( chart ) &gt;
	 * &lt;!ATTLIST fsc version NMTOKEN #REQUIRED &gt;
	 * 
	 * &lt;!ELEMENT chart ( text, lattice ) &gt;
	 * &lt;!ATTLIST chart id CDATA #REQUIRED &gt;
	 * 
	 * &lt;!ELEMENT text ( #PCDATA ) &gt;
	 * 
	 * &lt;!ELEMENT lattice ( edge* ) &gt;
	 * 
	 * &lt;!ATTLIST lattice final CDATA #REQUIRED &gt;
	 * &lt;!ATTLIST lattice init CDATA #REQUIRED &gt;
	 * 
	 * &lt;!ELEMENT edge ( fs ) &gt;
	 * &lt;!ATTLIST edge source CDATA #REQUIRED &gt;
	 * &lt;!ATTLIST edge target CDATA #REQUIRED &gt;
	 * 
	 * &lt;!ELEMENT fs ( f* ) &gt;
	 * &lt;!ATTLIST fs type CDATA #REQUIRED &gt;
	 * 
	 * &lt;!ELEMENT f ( fs | str )* &gt;
	 * &lt;!ATTLIST f name CDATA #REQUIRED &gt;
	 * &lt;!ATTLIST f org ( list ) #IMPLIED &gt;
	 * 
	 * &lt;!ELEMENT str ( #PCDATA ) &gt;
	 * </pre>
	 * 
	 */
	
	public void outputFSC () {
		
		if (countOfSents == 0) {
			log.info("No input sentence.");
			return;
		}
		String sent = sentences[0];
		int nTokens = tokens[0].length;
		String[] tokens = this.tokens[0];
		
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(System.out,of);
		// SAX2.0 ContentHandler.
		ContentHandler hd;
		try {
			hd = serializer.asContentHandler();
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			// <fsc version="1.0">
			atts.addAttribute("", "", "version", "CDATA", "1.0");
			hd.startElement("", "", "fsc", atts);
			
			// <chart id="fsc-test">
			atts.clear();
			atts.addAttribute("", "", "id", "CDATA", "fsc");
			hd.startElement("", "", "chart", atts);
			
			// <text>The dog chases the orc.</text>
			atts.clear();
			hd.startElement("", "", "text", atts);
			hd.characters(sent.toCharArray(), 0, sent.length());
			hd.endElement("", "", "text");
			
			// <lattice init="v0" final="v6">
			atts.clear();
			atts.addAttribute("", "", "init", "CDATA", "v0");
			atts.addAttribute("", "", "final", "CDATA", "v"+Integer.toString(nTokens));
			hd.startElement("", "", "lattice", atts);
			
			int tokenStart = 0;
			int tokenLen = 0;
			for (int i=0; i<nTokens; i++) {
				tokenLen = tokens[i].length();
				// <edge source="v0" target="v1">
				atts.clear();
				atts.addAttribute("", "", "source", "CDATA", "v"+Integer.toString(i));
				atts.addAttribute("", "", "target", "CDATA", "v"+Integer.toString(i+1));
				hd.startElement("", "", "edge", atts);
				
				// <fs type="token">
				atts.clear();
				atts.addAttribute("", "", "type", "CDATA", "token");
				hd.startElement("", "", "fs", atts);
				
				// <f name="+FORM"><str>The</str></f>
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+FORM");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				hd.characters(tokens[i].toCharArray(), 0, tokenLen);
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				// <f name="+FROM"><str>0</str></f>
				String num;
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+FROM");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				num = Integer.toString(tokenStart);
				hd.characters(num.toCharArray(), 0, num.length());
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				// <f name="+TO"><str>3</str></f>
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+TO");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				num = Integer.toString(tokenStart+tokenLen);
				hd.characters(num.toCharArray(), 0, num.length());
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				hd.endElement("", "", "fs");				
				hd.endElement("", "", "edge");
				
//				<f name="+TNT">
//	              <fs type="tnt">
//	                <f name="+TAGS" org="list"><str>DT</str></f>
//	                <f name="+PRBS" org="list"><str>1.000000e+00</str></f>
//	              </fs>
//	            </f>

				
				// 1 for a space
				tokenStart += (tokenLen+1);
			}
			
			
			
			hd.endElement("", "", "lattice");
			hd.endElement("", "", "chart");
			hd.endElement("", "", "fsc");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Output preprocessed sentence to FSC format by terms
	 * 
	 */
	public void outputFSCbyTerms (OutputStream os) {
		
		if (countOfSents == 0) {
			log.info("No input sentence.");
			return;
		}
		String sent = sentences[0];
		int nTokens = tokens[0].length;
		String[] tokens = this.tokens[0];
		Term[] terms = this.terms[0];
		
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(os,of);
		// SAX2.0 ContentHandler.
		ContentHandler hd;
		try {
			hd = serializer.asContentHandler();
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			// <fsc version="1.0">
			atts.addAttribute("", "", "version", "CDATA", "1.0");
			hd.startElement("", "", "fsc", atts);
			
			// <chart id="fsc-test">
			atts.clear();
			atts.addAttribute("", "", "id", "CDATA", "fsc");
			hd.startElement("", "", "chart", atts);
			
			// <text>Al Gore was born in Washington DC .</text>
			atts.clear();
			hd.startElement("", "", "text", atts);
			hd.characters(sent.toCharArray(), 0, sent.length());
			hd.endElement("", "", "text");
			
			// <lattice init="v0" final="v8">
			atts.clear();
			atts.addAttribute("", "", "init", "CDATA", "v0");
			atts.addAttribute("", "", "final", "CDATA", "v"+Integer.toString(nTokens));
			hd.startElement("", "", "lattice", atts);
			
			int tokenStart = 0;
			int tokenLen = 0;
			int step = 1;
			for (int i=0; i<nTokens; i+=step) {
				tokenLen = tokens[i].length();
				step = 1;
				Term term = null;
				
				for (Term t:terms) {
					if (t.getFrom() == i) {
						// tokens starting from i is a term
						step = t.getTo() - t.getFrom();
						term = t;
						break;
					} else if (t.getFrom() > i) {
						break;
					}
				}
				// <edge source="v0" target="v2">
				atts.clear();
				if (term == null) {
					atts.addAttribute("", "", "source", "CDATA", "v"+Integer.toString(i));
					atts.addAttribute("", "", "target", "CDATA", "v"+Integer.toString(i+1));
				} else {
					atts.addAttribute("", "", "source", "CDATA", "v"+term.getFrom());
					atts.addAttribute("", "", "target", "CDATA", "v"+term.getTo());
				}
				hd.startElement("", "", "edge", atts);
				
				// <fs type="token">
				atts.clear();
				atts.addAttribute("", "", "type", "CDATA", "token");
				hd.startElement("", "", "fs", atts);
				
				// <f name="+FORM"><str>Al Gore</str></f>
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+FORM");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				if (term == null) {
					hd.characters(tokens[i].toCharArray(), 0, tokenLen);
				} else {
					hd.characters(term.getText().toCharArray(), 0, term.getText().length());
				}
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				// <f name="+FROM"><str>0</str></f>
				String num;
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+FROM");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				// the same start from token and term
				num = Integer.toString(tokenStart);
				hd.characters(num.toCharArray(), 0, num.length());
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				// <f name="+TO"><str>7</str></f>
				atts.clear();
				atts.addAttribute("", "", "name", "CDATA", "+TO");
				hd.startElement("", "", "f", atts);
				atts.clear();
				hd.startElement("", "", "str", atts);
				if (term == null) {
					num = Integer.toString(tokenStart+tokenLen);
					hd.characters(num.toCharArray(), 0, num.length());
				} else {
					num = Integer.toString(tokenStart+term.getText().length());
					hd.characters(num.toCharArray(), 0, num.length());
				}
				hd.endElement("", "", "str");
				hd.endElement("", "", "f");
				
				if (term != null) {
	//				<f name="+TNT">
	//	              <fs type="tnt">
	//	                <f name="+TAGS" org="list"><str>DT</str></f>
	//	                <f name="+PRBS" org="list"><str>1.000000e+00</str></f>
	//	              </fs>
	//	            </f>
					atts.clear();
					atts.addAttribute("", "", "name", "CDATA", "+TNT");
					hd.startElement("", "", "f", atts);
					atts.clear();
					atts.addAttribute("", "", "type", "CDATA", "tnt");
					hd.startElement("", "", "fs", atts);
					
					// <f name="+TAGS" org="list"><str>DT</str></f>
					atts.clear();
					atts.addAttribute("", "", "name", "CDATA", "+TAGS");
					atts.addAttribute("", "", "org", "CDATA", "list");
					hd.startElement("", "", "f", atts);
					atts.clear();
					hd.startElement("", "", "str", atts);
					hd.characters(term.getPosFSC().toCharArray(), 0, term.getPosFSC().length());
					hd.endElement("", "", "str");
					hd.endElement("", "", "f");
					
					//<f name="+PRBS" org="list"><str>1.000000e+00</str></f>
					atts.clear();
					atts.addAttribute("", "", "name", "CDATA", "+PRBS");
					atts.addAttribute("", "", "org", "CDATA", "list");
					hd.startElement("", "", "f", atts);
					atts.clear();
					hd.startElement("", "", "str", atts);
					hd.characters("1.000000e+00".toCharArray(), 0, "1.000000e+00".length());
					hd.endElement("", "", "str");
					hd.endElement("", "", "f");
					
									
					hd.endElement("", "", "fs");
					hd.endElement("", "", "f");
					
				}
				
				hd.endElement("", "", "fs");				
				hd.endElement("", "", "edge");
				
				if (term == null) {
					// 1 for a space
					tokenStart += (tokenLen+1);
				} else {
					tokenStart += (term.getText().length()+1);
				}
			}
			
			hd.endElement("", "", "lattice");
			hd.endElement("", "", "chart");
			hd.endElement("", "", "fsc");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	
	public static void addDictionary(Dictionary dict) {
		dicts.add(dict);
	}
	
	/**
	 * Returns the <code>Dictionaries</code>.
	 * 
	 * @return dictionaries
	 */
	public static Dictionary[] getDictionaries() {
		return dicts.toArray(new Dictionary[dicts.size()]);
	}	
	/**
	 * Unregisters all <code>Dictionaries</code>.
	 */
	public static void clearDictionaries() {
		dicts.clear();
	}
	
	public static void main(String[] args) {
		String answers  = "Al Gore was born in Washington DC. Al Gore lives in Washington DC.";
		Preprocessor t = new Preprocessor();
		// possibly fail because of dict is not loaded
		t.preprocess(answers);
	}

}
