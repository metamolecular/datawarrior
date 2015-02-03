package com.actelion.research.gui.util;

import java.awt.Component;
import java.awt.Frame;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Utility class to display an error and its causes, in a JOptionPane
 * @author freyssj
 *
 */
public class JExceptionDialog {

	private JExceptionDialog() {}
	
	public static void show(Throwable e) {
		Frame parent = Frame.getFrames().length > 0 ? Frame.getFrames()[Frame.getFrames().length - 1] : null;
		show(parent, e);
	}
	
	public static void show(String s) {
		Frame parent = Frame.getFrames().length > 0 ? Frame.getFrames()[Frame.getFrames().length - 1] : null;
		show(parent, s);
	}
	
	public static void show(Component parent, Throwable e) {
		if(e==null) throw new IllegalArgumentException("The error cannot be null");
		e.printStackTrace();
		ApplicationErrorLog.logException(e);
		StringBuilder sb = new StringBuilder();
		while(e!=null) {
			sb.append(e.getMessage() + "\n");
			e = e.getCause();
		}
		show(parent, sb.toString());
	}
	
	public static void show(Component parent, List<String> messages) {
		StringBuilder sb = new StringBuilder();
		sb.append("Some problems were found:\n");
		int i = 0;
		for (String s : messages) {
			sb.append(" - " + s + "\n");
			if(i++>10) {
				sb.append(messages.size()-10+" more...");
				break;
			}
		}
		show(parent, sb.toString());
	}
	
	public static void show(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	public static void showInfo(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	public static void showWarning(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
	}
}
