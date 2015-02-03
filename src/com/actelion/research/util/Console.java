/*
 * Created on Apr 6, 2005
 *
 */
package com.actelion.research.util;

/**
 * 
 * @author FreyssJ
 */
// Well, this seems to be a logging function, is that necessary?
public class Console {

	public static final int ERROR = 0;
	public static final int INFO = 1;
	public static final int HIGHLIGHT = 2;
	public static final int DEBUG = 3;

	public void write(int type, String message) {
		switch(type) {
			case ERROR: System.err.print(message); break;
			default: System.out.print(message);
		}
	}
	public void writeln(int type, String message) {
		write(type, message + System.getProperty("line.separator")); 
	}
	public void newSection(String title) {
		writeln(Console.INFO, title); 
	}

}
