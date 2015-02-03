package com.actelion.research.util;

import java.io.*;


/**
 * 
 * @author freyssj
 */
public class IOUtils {

	public static void saveObject(String file, Object o) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		ObjectOutputStream s = new ObjectOutputStream(out);
		s.writeObject(o);
		out.close();
	}
	
	public static Object loadObject(String file) throws IOException, ClassNotFoundException {
		FileInputStream in = new FileInputStream(file);
		ObjectInputStream s = new ObjectInputStream(in);
		Object o = s.readObject();
		in.close();
		return o;
	}

	public static String readerToString(Reader reader) throws IOException {
		return  readerToString(reader, Integer.MAX_VALUE);
	}
	public static String readerToString(Reader reader, int maxSize) throws IOException {
		char[] buf = new char[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(sb.length()<maxSize && ( c = reader.read(buf, 0, Math.min(buf.length, maxSize-sb.length()))) > 0) {			
			sb.append(buf, 0, c);
		}
		return sb.toString();
	}		

	public static String streamToString(InputStream is) throws IOException {
		byte[] buf = new byte[512];
		int c;
		StringBuilder sb = new StringBuilder();
		while(( c = is.read(buf)) > 0) {
			sb.append(new String(buf, 0, c));
		}
		return sb.toString();
	}		
	
	public static byte[] getBytes(File f) throws IOException {
		FileInputStream is = new FileInputStream(f);
		byte[] res = new byte[(int) f.length()];
		is.read(res, 0, res.length);
		is.close();
		return res;
	}		
	
	public static void bytesToFile(byte[] bytes, File f) throws IOException {
		FileOutputStream os = new FileOutputStream(f);
		os.write(bytes);
		os.close();
	}
	
	public static void stringToFile(String s, File f) throws IOException {
		FileWriter os = new FileWriter(f);
		os.write(s);
		os.close();
	}
	
	public static void saveStream(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[512];
		int c;
		while(( c = is.read(buf)) > 0) {
			os.write(buf, 0, c);
		}
	}		

	public static void redirect(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
		is.close();
	}	

	public static void redirect(Reader is, Writer os) throws IOException {
		char[] buf = new char[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
		is.close();
	}	

	public static void redirectStream(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[512];
		int c;
		while((c=is.read(buf))>0) {
			os.write(buf, 0, c);
		}
		is.close();
	}	

}
