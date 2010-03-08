package com.googlecode.mrsqg.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Cheap {
    static Process p;
    static OutputStream pin = null;
    static InputStream pout = null;
    
    public Cheap (String path) {
    	try {
    		String[] cmd = {"/bin/sh", "-c", path};
    		p = Runtime.getRuntime().exec(cmd);
    		pin = p.getOutputStream();
    		//pout = p.getErrorStream();
    		pout = p.getInputStream();
    	} catch (IOException e) {
            e.printStackTrace();
            pin = null;
            pout = null;
    	}
    }
    
    public String parse(String sentence) {
        if((pin == null) || (pout == null)) {
            return "";
        }
        
        sentence = sentence+"\n";
        
        try {
            pin.write(sentence.getBytes());
            pin.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        StringBuffer output = new StringBuffer();

        BufferedReader br = new BufferedReader(new InputStreamReader(pout));

        String line = null;

        try {
        	while((line = br.readLine()) != null) {
        		output.append(line);
        		output.append("\n");
        		if(line.length() < 3) {
        			break;
        		}
        	}
        } catch (IOException e2) {
                e2.printStackTrace();
        }

        return output.toString();
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Cheap parser = new Cheap("cheap  -default-les=all -cm -packing -mrs -results=1 " +
//				"/home/xcyao/delphin/erg/english.grm");
		Cheap parser = new Cheap("cat");
		String output = parser.parse("John likes Mary.");
		
		System.out.println("Output:\n");
		System.out.println(output);
		System.out.println("Done");
	}

}
