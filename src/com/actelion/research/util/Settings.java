package com.actelion.research.util;

import java.io.*;

public class Settings {
	public static String getProperty(String key) {
		try {
			String filepath = getUserHome() + "." + key;
			BufferedReader r = new BufferedReader(new FileReader(filepath));
            StringBuffer value = new StringBuffer();
            String line = r.readLine();
            while (line != null) {
                if (value.length() != 0)
                    value.append('\n');
                value.append(line);
                line = r.readLine();
                }
			return value.toString();
			}
		catch (IOException ioe) {
			return null;
			}
		}

	public static void setProperty(String key, String property) {
		try {
			String filepath = getUserHome() + "." + key;
			BufferedWriter w = new BufferedWriter(new FileWriter(filepath));
			w.write(property);
			w.newLine();
			w.close();
			}
		catch (IOException ioe) {}
		}

	private static String getUserHome() {
		return System.getProperty("user.home")
			 + System.getProperty("file.separator");
		}
	}
