package com.googlecode.mrsqg.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.googlecode.mrsqg.mrs.MRS;


public class Cheap {
	
	private static Logger log = Logger.getLogger(Cheap.class);
	public final String propertyFile = "conf/cheap.properties";
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	/** whether cheap is loaded successfully */
	private boolean success = false;
	
	/**
	 * Cheap constructor 
	 * @param fsc a boolean value indicating weather cheap takes FSC as input format
	 */
	public Cheap(boolean fsc) {
		Properties prop = new Properties();
		try { 
			prop.load(new FileInputStream(propertyFile)); 
		} catch (IOException e) {
			e.printStackTrace();
		} 
		String command;
		if (fsc) {
			command = prop.getProperty("cheap");
		} else {
			command = prop.getProperty("cheap_test");
		}
		
		if (!command.contains("-mrs=mrx")) {
			log.fatal("the cheap cmd line paramater must contain" +
					" the -mrs=mrx option! Check your conf/cheap.properties file!");
		}
		
		if (fsc) {
			if (!command.contains("-tok=fsc")) {
				log.fatal("the cheap cmd line paramater must contain" +
						" the -tok=fsc option! Check your conf/cheap.properties file!");
			}
		}
		
		try {
			log.info("Cheap is starting up, please wait...\n ");
			p = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// output cheap loading message
		try {
			ErrorReader err = new ErrorReader();
			err.start();
			String buff = getError();
			log.info(buff);
		} catch (Exception e) {
			e.printStackTrace();
		}
		success = true;
	}
	
	/**
	 * Whether the parser is started successfully
	 * @return a boolean status
	 */
	public boolean isSuccess () { return success;}
	
	/**
	 * Parse an input in FSC XML format
	 */
	public void parse (String input) {
		if (!success) {
			log.fatal("cheap is not working properly!");
			return;
		}
		InputWriter in = new InputWriter(input);
		in.start();
	}
	
	/**
	 * Get parsing result
	 * @return the parsing result
	 */
	public String getParseResult () {
		if (!success) {
			log.fatal("cheap is not working properly!");
			return null;
		}
		// cheap directs all output to stderr
		//OutputReader out = new OutputReader();
		//out.start();
		ErrorReader err = new ErrorReader();
		err.start();

		String result = getError();
		return result;
	}
	
	/**
	 * 
	 * Retrieve a list of <mrs> elements from the parsing result.
	 * Warning: Cannot be used simultaneously with getParseResult
	 * 
	 * @return an ArrayList<String> with each member containing a <mrs> element
	 */
	public ArrayList<String> getParsedMrxString () {
		if (!success) {
			log.fatal("cheap is not working properly!");
			return null;
		}
		// cheap directs all output to stderr
		//OutputReader out = new OutputReader();
		//out.start();
		ErrorReader err = new ErrorReader();
		err.start();

		String result = getError();
		if (result==null) return null;
		
		return MRS.getMrxStringsFromCheap(result);
	}
	
	/**
	 * Retrieve a list of MRS objects from the parsing result.
	 * Warning: Cannot be used simultaneously with getParseResult and getParsedMrxString
	 * 
	 * @return an ArrayList<MRS> with each member containing an MRS object
	 * 
	 */
	public ArrayList<MRS> getParsedMRSlist () {
		ArrayList<String> mrxList = getParsedMrxString();
		if (mrxList == null || mrxList.size()==0) return null;
		ArrayList<MRS> list = new ArrayList<MRS> ();
		for (String s:mrxList) {
			MRS m = new MRS();
			m.parseString(s);
			list.add(m);
		}
		
		return list;
	}
	
	/**
	 * exit the parser properly
	 */
	public void exit () {
		if (!success) {
			log.fatal("cheap is not working properly!");
			return;
		}
		// a cr makes cheap exit
		parse("\n");
	}
	
	public static void main(String args[]) {
		PropertyConfigurator.configure("conf/log4j.properties");
		Cheap parser = new Cheap(false);
		
		if (! parser.isSuccess()) {
			log.fatal("cheap is not started properly.");
			return;
		}
		
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				parser.exit();
				System.exit(0);
			}
			parser.parse(input);
			//System.out.println(parser.getParseResult());
			System.out.println("\nParsed MRS:\n");
			//System.out.println(parser.getParsedMrxString());
			System.out.println(parser.getParsedMRSlist());
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
		public OutputReader() {
			try {
				outputSem = new Semaphore(1);
				outputSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p
						.getInputStream()));
				String buff = new String();

				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
					//System.out.println("Output in readline: "+buff);
				}
				output = readBuffer.toString();
				System.out.println("Output in MainThread: "+output);
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
					//System.out.println("Error in readline: "+buff);
					// Jail Break! 
					// The other side doesn't close so readLine() will never return null. 
					if (buff.contains("cheap is brutally patched")) break;
					// finishing loading message:  "92441 types in 10 s" 
					if (buff.contains(" types in ")) {
						//log.info(readBuffer.toString());
						break;
					}
				}
				error = readBuffer.toString();
				//System.out.println("Error in MainThread: "+error);
				errorSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			if (error.length() > 0)
//				System.out.println("Output: "+error);
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
		if (value.contains("no lexicon entries for:")) {
			log.error("Cheap output:");
			log.error(value);
			return null;
		}
		return value;
	}

}

/*
package com.googlecode.mrsqg.nlp;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Cheap {
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	
	public static void main(String args[]) {
		Cheap e = new Cheap("cat");
		
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
		public OutputReader() {
			try {
				outputSem = new Semaphore(1);
				outputSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p
						.getInputStream()));
				String buff = new String();
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
					//System.out.println("Output: "+buff);
				}
				output = readBuffer.toString();
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
					readBuffer.append(buff);
				}
				error = readBuffer.toString();
				errorSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (error.length() > 0)
				System.out.println("Output: "+error);
		}
	}

	public Cheap(String command, String input) {
		try {
			p = Runtime.getRuntime().exec(makeArray(command));
			new InputWriter(input).start();
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
	public Cheap(String command) {
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				System.exit(0);
			}
			try {
				p = Runtime.getRuntime().exec(command);
				new InputWriter(input).start();
				new OutputReader().start();
				new ErrorReader().start();
				//String buff = getOutput();
				//System.out.println("Output: "+buff);
				//p.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	private String[] makeArray(String command) {
		ArrayList<String> commandArray = new ArrayList<String>();
		String buff = "";
		boolean lookForEnd = false;
		for (int i = 0; i < command.length(); i++) {
			if (lookForEnd) {
				if (command.charAt(i) == '\"') {
					if (buff.length() > 0)
						commandArray.add(buff);
					buff = "";
					lookForEnd = false;
				} else {
					buff += command.charAt(i);
				}
			} else {
				if (command.charAt(i) == '\"') {
					lookForEnd = true;
				} else if (command.charAt(i) == ' ') {
					if (buff.length() > 0)
						commandArray.add(buff);
					buff = "";
				} else {
					buff += command.charAt(i);
				}
			}
		}
		if (buff.length() > 0)
			commandArray.add(buff);

		String[] array = new String[commandArray.size()];
		for (int i = 0; i < commandArray.size(); i++) {
			array[i] = commandArray.get(i);
		}

		return array;
	}
}

*/