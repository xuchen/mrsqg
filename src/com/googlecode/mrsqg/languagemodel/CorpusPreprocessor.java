/**
 * This class searches all plain text corpus with a ".clean" suffix
 * in a directory, tokenize all words, add sentence boundaries (<s>,</s>),
 * and output the result to a single file.
 *
 * OpenNLP tokenizer is required to prepare the corpus since MrsQG also
 * uses OpenNLP tokenizer. We need the same tool to provide the same
 * tokenization.
 */
package com.googlecode.mrsqg.languagemodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import com.googlecode.mrsqg.nlp.OpenNLP;

/**
 * @author Xuchen Yao
 *
 */
public class CorpusPreprocessor {

	/** Directory containing all plain-text files */
	private File inputDir;

	/** training file */
	private File trainFile;

	/** test file, (e.g. 10% of data) randomly distributed */
	private File testFile;

	private BufferedWriter outTrain;
	private BufferedWriter outTest;

	/** how much of all data is split into test data */
	private double thresh = 0.1;

	public CorpusPreprocessor (String dir, String trainFile, String testFile) {
		if (!OpenNLP.createTokenizer("res/nlp/tokenizer/opennlp/EnglishTok.bin.gz"))
			System.err.println("Could not create tokenizer.");
		this.inputDir = new File(dir);
		this.trainFile = new File(trainFile);
		this.testFile = new File(testFile);
		try {
			// open by appending
			outTrain = new BufferedWriter(new FileWriter(trainFile, true));
			outTest = new BufferedWriter(new FileWriter(testFile, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * http://vafer.org/blog/20071112204524
	 * Recursively travels a directory.
	 * @param f A file or directory
	 * @throws IOException
	 */
	public final void traverse( final File f ) throws IOException {
		if (f.isDirectory()) {
			onDirectory(f);
			final File[] childs = f.listFiles();
			for( File child : childs ) {
				traverse(child);
			}
			return;
		}
		onFile(f);
	}

	private void onDirectory( final File d ) {
	}

	private void onFile( final File f ) {
		if (f.getName().endsWith(".clean")) {
			try {
				StringBuilder sbTrain = new StringBuilder();
				StringBuilder sbTest = new StringBuilder();
				String line;
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f), "UTF-8"));
				while (in.ready()) {
					line = in.readLine().trim().toLowerCase(Locale.ENGLISH);
					if (line.length()==0) continue;
					line = OpenNLP.tokenizeWithSpaces(line);
					if (Math.random()>this.thresh)
						sbTrain.append("<s> "+line+" </s>\n");
					else
						sbTest.append("<s> "+line+" </s>\n");
				}
				if (sbTrain.length()>0)
					this.outTrain.write(sbTrain.toString());
				if (sbTest.length()>0)
					this.outTest.write(sbTest.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			return;
	}

	public void doIt() {
		try {
			traverse(this.inputDir);
			outTrain.close();
			outTest.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		CorpusPreprocessor p = new CorpusPreprocessor(
				"/home/xcyao/openAryhpe/QAcorpus",
				"/home/xcyao/openAryhpe/QAcorpus/questions.train",
				"/home/xcyao/openAryhpe/QAcorpus/questions.test");
		p.doIt();

	}

}
