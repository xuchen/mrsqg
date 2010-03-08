package com.googlecode.mrsqg.nlp;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ExecCommand {
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	
	public static void main(String args[]) {
		ExecCommand e = new ExecCommand("cheap  -default-les=all -cm -packing -mrs -results=1 " +
				"/home/xcyao/delphin/erg/english.grm");
		
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
				/*
				int c;
				while ((c=isr.read())!=-1){
					buff = String.valueOf(c);
					readBuffer.append(buff);
					System.out.println("Output in readline: "+buff);
					if (c==10) break;
				}
				*/
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
					System.out.println("Output in readline: "+buff);
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
					System.out.println("Error in readline: "+buff);
					// Jail Break! 
					// The other side doesn't close so readLine() will never return null. 
					if (buff.contains("HCONS")) break;
				}
				error = readBuffer.toString();
				System.out.println("Error in MainThread: "+error);
				errorSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			if (error.length() > 0)
//				System.out.println("Output: "+error);
		}
	}

	public ExecCommand(String command, String input) {
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
	public ExecCommand(String command) {
		try {
			// Xuchen Yao
			// the command actually runs as a "server"
			p = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while (true) {
			System.out.println("Input: ");
			String input = readLine().trim();
			if (input.length() == 0) continue;
			if (input.equalsIgnoreCase("exit")) {
				System.exit(0);
			}
			try {
				InputWriter in = new InputWriter(input);
				in.start();
				OutputReader out = new OutputReader();
				out.start();
				ErrorReader err = new ErrorReader();
				err.start();
				// Xuchen Yao
				// Uncomment the following makes the system not work
				String buff = getError();
				System.out.println("Erro in Main: "+error);
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

/*
package com.googlecode.mrsqg.nlp;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ExecCommand {
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;
	
	public static void main(String args[]) {
		ExecCommand e = new ExecCommand("cat");
		
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

	public ExecCommand(String command, String input) {
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
	public ExecCommand(String command) {
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