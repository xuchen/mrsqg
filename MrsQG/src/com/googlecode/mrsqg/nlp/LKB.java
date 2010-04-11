package com.googlecode.mrsqg.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class LKB {
	
	private static Logger log = Logger.getLogger(LKB.class);
	public final String propertyFile = "conf/lkb.properties";
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	/** whether LKB is loaded successfully */
	private boolean success = false;
	private boolean display;
	
	/**
	 * LKB constructor 
	 * @param quicktest true to only load LKB for testing purposes, 
	 * false to also load ERG and generate index (which takes a while).
	 */
	public LKB(boolean quicktest) {
		String genCmd = "(index-for-generator)";
		Properties prop = new Properties();

		try { 
			prop.load(new FileInputStream(propertyFile)); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		String scriptFile = prop.getProperty("script");
		File f = new File(scriptFile);
		if (!f.exists()) {
			log.fatal("File "+scriptFile+" should exist!");
			return;
		}
		String scriptCmd = "(read-script-file-aux \""+scriptFile+"\")";
		
		String lkb = prop.getProperty("lkb");
		f = new File(lkb);
		if (!f.exists()) {
			log.fatal("LKB "+scriptFile+" should exist!");
			return;
		}
		
		if (prop.getProperty("display").equalsIgnoreCase("yes")) {
			display = true;
		} else if (prop.getProperty("display").equalsIgnoreCase("no")) {
			display = false;
		} else {
			log.error("the display option in conf/lkb.properties is not " +
					"set properly: "+prop.getProperty("display")+". " +
							"Assuming it's yes");
			display = true;
		}
		
		if (!display) {
			lkb="DISPLAY=;"+lkb;
		}
		
		try {
			log.info("LKB is starting up, please wait, wait, wait until you see \"Input: \"...\n");
			String[] cmd = {"/bin/sh","-c",lkb};
			p = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// load script and generate index for the generator
		if (!quicktest) {
			sendInput(scriptCmd+genCmd);
		}
		
		// output LKB loading message
		success = true;
		// 3 commands were sent: 
		// one for LKB itself,
		// one for scriptCmd,
		// one for genCmd.
		// Thus 3 threads are needed to retrieve LKB output
		log.info(getRawOutput());
		if (!quicktest) {
			String out = getRawOutput()+"\n"+getRawOutput();
			log.info(out);
			if (out.contains("select using :continue")) {
				success = false;
				log.fatal("Fatal error: LKB didn't start properly." +
						" Press Enter and try again.");
				System.out.println("Press Enter: ");
				readLine();
			}
		}
		if (!quicktest && success)
			log.info("Initializing LKB done. Quite a while, huh?;-)\n");
	}
	
	/**
	 * Whether the parser is started successfully
	 * @return a boolean status
	 */
	public boolean isSuccess () { return success;}
	
	/**
	 * Whether LKB is set to show display.
	 * @return true or false
	 */
	public boolean getDisplay () { return display;}
	
	/**
	 * Send an input string to LKB
	 */
	public void sendInput (String input) {
		//String cmd = input.replaceAll("\n","").replaceAll("\"", "\\\\\"");
		InputWriter in = new InputWriter(input);
		in.start();
	}
	
	/**
	 * Get result from stdout
	 * @return the parsing result
	 */
	public String getRawOutput () {
		if (!success) {
			log.fatal("LKB is not working properly!");
			return null;
		}

		OutputReader out = new OutputReader();
		out.start();

		String result = getOutput();
		return result;
	}
	
	/**
	 * Parse a raw LKB output and return all generated sentences
	 * @param raw a raw LKB output
	 * @return an ArrayList of generated sentences in raw
	 */
	public static ArrayList<String> parseGen (String raw) {

		// sample raw generation output:
/*
 		raw = "(\"Who was killed in Gary , Indiana on August 29 , 1958?\"\n" +
				" \"Who was killed on August 29 , 1958 in Gary , Indiana?\")\n" +
				"114832\n7517\n1080\n7843\n1049\n444\n726\n";
				
("Who was killed in Gary , Indiana on August 29 , 1958?"
 "Who was killed on August 29 , 1958 in Gary , Indiana?")
114832
7517
1080
7843
1049
444
726

or:
("Al Gore does live in Washington DC."
 "Al Gore does live in Washington DC." ...)
 114832
7517
1080
7843
1049
444
726

After proper formatting in sendMrsToGen(), it looks like:
("Where does Al Gore live in?", "Where does Al Gore live in?", "In where does Al Gore live?")
NIL
*/
		ArrayList<String> genList = new ArrayList<String>();
		
		Pattern gen = Pattern.compile("\\(\"(.*)\"\\)\nNIL", 
				Pattern.MULTILINE|Pattern.DOTALL);

		Matcher m = gen.matcher(raw);
		String genStr;
		
		if (m.find()) {
			genStr = m.group(1);
		} else {
			// for null, it generates:
			//()
			//NIL
			Pattern nil = Pattern.compile("\\(\\)\nNIL", 
					Pattern.MULTILINE|Pattern.DOTALL);
			m = nil.matcher(raw);
			if (m.find()) {
				log.warn("No generation from LKB");
				log.warn("LKB output:\n"+raw);
				return null;
			} else {
				log.warn("No matching, probably due to LKB generation failure!");
				log.warn("LKB output:\n"+raw);
				return null;
			}
		}
		
		String[] list = genStr.split("\", \"");
		
		if(list==null) {
			log.warn("No split matching, debug your code!");
			log.warn("generation String:\n"+genStr);
			return null;
		}
		for (String s:list) {
			genList.add(s);
		}
		
		//System.out.println(genList);
		return genList.size()==0 ? null : genList;
	}
	
	/**
	 * Get generated sentences after calling sendMrxToGen
	 * Warning: this function can only be called once!
	 * calling more than once WILL BLOCK THE PROGRAM!
	 * @return an ArrayList of generated sentences in raw
	 */
	public ArrayList<String> getGenSentences () {
		String raw = getRawOutput();
		if (raw==null) return null;
		return parseGen(raw);
	}
	
	/**
	 * In case of a generation failure, send the (print-gen-summary)
	 * cmd to LKB to get all the excerpts of *gen-chart*
	 * @return an ArrayList containing all the edges of *gen-chart*
	 */
	public ArrayList<String> getFailedGenSentences () {
		sendInput("(print-gen-summary)");
		String raw = getRawOutput();
		return parseFailedGen(raw);
	}
	
	/**
	 * From the cmd (print-gen-summary) LKB produces output like this:
LKB(5): (print-gen-summary)
------
(Al Gore)
(AL GORE,)
(AL GORE.)
(AL GORE?)
(do)

NIL
LKB(6): 

	 * This function accepts a raw string like above and return the list
	 * of all strings inside ().
	 * @param raw a raw String from the cmd (print-gen-summary)
	 * @return a parsed ArrayList
	 */
	public static ArrayList<String>  parseFailedGen(String raw) {
		ArrayList<String> genList = new ArrayList<String>();
		
		// match the whole string
		Pattern genAll = Pattern.compile("-+.*NIL", 
				Pattern.MULTILINE|Pattern.DOTALL);
		
		Matcher mAll = genAll.matcher(raw);
		String genStr;
		
		if (mAll.find()) {
			Pattern gen = Pattern.compile("\\((.*?)\\)\n");
			Matcher m = gen.matcher(raw);
			while (m.find()) {
				genStr = m.group(1);
				genList.add(genStr);
			}
			if (genList.size()==0) {
				log.warn("No matching, no chart in *gen-chart* ?");
				log.warn("LKB output:\n"+raw);
				return null;
			}
		} else {
			log.warn("No matching, (print-gen-summary) failed to run?");
			log.warn("LKB output:\n"+raw);
			return null;
		}
		


		return genList.size()==0 ? null : genList;
	}
	
	/**
	 * Send an MRX string to the LKB generator
	 * @param mrx A string containing an MRS in XML format
	 */
	public void sendMrxToGen (String mrx) {
		// a quote " in an LKB string needs to be escaped 
		String mrxCmd = mrx.replaceAll("\n","").replaceAll("\"", "\\\\\"");
		
		// (format t "(~{\"~a\"~^, ~})" list)
		// oh yeah, this is pain......
		mrxCmd = "(format t \"(~{\\\"~a\\\"~\\^, ~})\" (lkb::generate-from-mrs (mrs::read-single-mrs-xml-from-string " +
				"\""+mrxCmd+"\")))";
		
		sendInput(mrxCmd);
	}
	
	/**
	 * Get result from stderr. It seems LKB doesn't output anything 
	 * to stderr. So using this function will block the whole program.
	 * @return the parsing result
	 */
	public String getErrResult () {
		if (!success) {
			log.fatal("LKB is not working properly!");
			return null;
		}

		ErrorReader err = new ErrorReader();
		err.start();

		String result = getError();
		return result;
	}

	/**
	 * exit LKB
	 */
	public void exit () {
		if (!success) {
			log.fatal("LKB is not working properly!");
			return;
		}
		
		// force exit
		sendInput("(excl:exit 0 :no-unwind t :quiet t)\n");
	}
	
	public static void main(String args[]) {
				
		PropertyConfigurator.configure("conf/log4j.properties");
		boolean quicktest = true;
		LKB lkb = new LKB(quicktest);
		
		if (! lkb.isSuccess()) {
			log.fatal("LKB was not started properly.");
			return;
		}
		
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				lkb.exit();
				System.exit(0);
			}
			if (quicktest) {
				lkb.sendInput(input);
				System.out.println(lkb.getRawOutput());
			} else {
				lkb.sendMrxToGen(input);
				System.out.println(lkb.getGenSentences());
				//System.out.println(lkb.getFailedGenSentences());
			}
		}
	}

	private class InputWriter extends Thread {
		private String input;

		public InputWriter(String input) {
			this.input = input;
		}

		public void run() {
			PrintWriter pw = new PrintWriter(p.getOutputStream());
			pw.println(input);
			pw.flush();
		}
	}

	private class OutputReader extends Thread {
        private static final int colon = (int)':';
        private static final  int rightp = (int)')';
        private static final  int space = (int)' ';
        private Pattern prompt;
        
		public OutputReader() {
			prompt = Pattern.compile(".*LKB\\(\\d+\\): $", Pattern.MULTILINE|Pattern.DOTALL);
			try {
				outputSem = new Semaphore(1);
				outputSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			// previous previous history
			int pph=-1;
			// previous history
			int ph=-1;
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p
						.getInputStream()));
				String buff = new String();
				String jailbreak=null;
				
                int c;

                while ((c=isr.read())!=-1){

                	//buff = String.valueOf(c);
                	buff = Character.toString((char)c);
                	readBuffer.append(buff);
                	//System.out.println("Output in readline: "+buff);
                	if (c==space&&ph==colon&&pph==rightp) {
                		// Jail break!
                		// When LKB ends its output, it prompts: 
                		// LKB(1): (with a space in the end) 
                		// for efficiency reasons, we only compare int values first,
                		// then do a regex match
                		jailbreak = readBuffer.toString();
                		Matcher m = prompt.matcher(jailbreak);
                		if (m.matches()) 
                			break;
                	}
                	pph=ph;
                	ph=c;
                }

				output = jailbreak;
				//System.out.println("OutputReader Thread: "+output);
				outputSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ErrorReader extends Thread {
		public ErrorReader() {
			try {
				errorSem = new Semaphore(1);
				errorSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p
						.getErrorStream()));
				String buff = new String();
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff+"\n");
					log.info("Error in stderr of LKB: "+buff);
					log.info("System probably has hanged. " +
							"Add jail-breaking code to escape!");
				}
				error = readBuffer.toString();
				System.out.println("ErrorReader Thread: "+error);
				errorSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	protected static String readLine() {
		try {
			return new java.io.BufferedReader(new
				java.io.InputStreamReader(System.in)).readLine();
		}
		catch(java.io.IOException e) {
			return new String("");
		}
	}
	


	public String getOutput() {
		try {
			outputSem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String value = output;
		outputSem.release();
		return value;
	}

	public String getError() {
		try {
			errorSem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String value = error;
		errorSem.release();
		return value;
	}

}