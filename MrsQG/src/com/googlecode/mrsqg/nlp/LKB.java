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

import com.googlecode.mrsqg.mrs.MRS;


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
	
	/**
	 * LKB constructor 
	 * @param quicktest true to only load LKB for testing purposes, 
	 * false to also load ERG and generate index (which takes a while).
	 */
	public LKB(boolean quicktest) {
		String genCmd = "(index-for-generator)";
		Properties prop = new Properties();
		boolean display;
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
			log.info("LKB is starting up, please wait, wait, wait...");
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
		log.info(getResult());
		if (!quicktest) {
			log.info(getResult());
			log.info(getResult());
		}
		log.info("Initializing LKB done. Quite a while, huh?;-)\n");
	}
	
	/**
	 * Whether the parser is started successfully
	 * @return a boolean status
	 */
	public boolean isSuccess () { return success;}
	
	/**
	 * Send an input string to LKB
	 */
	public void sendInput (String input) {
		InputWriter in = new InputWriter(input);
		in.start();
	}
	
	/**
	 * Get result from stdout
	 * @return the parsing result
	 */
	public String getResult () {
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
		sendInput("(excl:exit 0 :no-unwind t)\n");
	}
	
	public static void main(String args[]) {
		PropertyConfigurator.configure("conf/log4j.properties");
		LKB lkb = new LKB(true);
		
		if (! lkb.isSuccess()) {
			log.fatal("LKB is not started properly.");
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
			lkb.sendInput(input);
			System.out.println(lkb.getResult());
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