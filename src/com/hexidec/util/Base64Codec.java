/*
GNU Lesser General Public License

Base64Codec - Base64 Document Encoding/Decoding Class
Copyright (C) 2003  Howard Kistler & other contributors

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.hexidec.util;

import java.util.Vector;

public class Base64Codec
{
//  Constants -------------------------------------------------------------------------------/

	public static Vector<String> Base64Tokens = new Vector<String>(64);
	static
	{
		Base64Tokens.add("A");
		Base64Tokens.add("B");
		Base64Tokens.add("C");
		Base64Tokens.add("D");
		Base64Tokens.add("E");
		Base64Tokens.add("F");
		Base64Tokens.add("G");
		Base64Tokens.add("H");
		Base64Tokens.add("I");
		Base64Tokens.add("J");
		Base64Tokens.add("K");
		Base64Tokens.add("L");
		Base64Tokens.add("M");
		Base64Tokens.add("N");
		Base64Tokens.add("O");
		Base64Tokens.add("P");
		Base64Tokens.add("Q");
		Base64Tokens.add("R");
		Base64Tokens.add("S");
		Base64Tokens.add("T");
		Base64Tokens.add("U");
		Base64Tokens.add("V");
		Base64Tokens.add("W");
		Base64Tokens.add("X");
		Base64Tokens.add("Y");
		Base64Tokens.add("Z");
		Base64Tokens.add("a");
		Base64Tokens.add("b");
		Base64Tokens.add("c");
		Base64Tokens.add("d");
		Base64Tokens.add("e");
		Base64Tokens.add("f");
		Base64Tokens.add("g");
		Base64Tokens.add("h");
		Base64Tokens.add("i");
		Base64Tokens.add("j");
		Base64Tokens.add("k");
		Base64Tokens.add("l");
		Base64Tokens.add("m");
		Base64Tokens.add("n");
		Base64Tokens.add("o");
		Base64Tokens.add("p");
		Base64Tokens.add("q");
		Base64Tokens.add("r");
		Base64Tokens.add("s");
		Base64Tokens.add("t");
		Base64Tokens.add("u");
		Base64Tokens.add("v");
		Base64Tokens.add("w");
		Base64Tokens.add("x");
		Base64Tokens.add("y");
		Base64Tokens.add("z");
		Base64Tokens.add("0");
		Base64Tokens.add("1");
		Base64Tokens.add("2");
		Base64Tokens.add("3");
		Base64Tokens.add("4");
		Base64Tokens.add("5");
		Base64Tokens.add("6");
		Base64Tokens.add("7");
		Base64Tokens.add("8");
		Base64Tokens.add("9");
		Base64Tokens.add("+");
		Base64Tokens.add("/");
	}	
	public static final char BASE64PAD = '=';
	public static final char LINEFEED  = (char)10;
	public static final char CARRIAGE  = (char)13;
	public static final int  LINEMAX   = 75;

//  Constructors ----------------------------------------------------------------------------/

	public Base64Codec()
	{
	}

//  Processor Methods -----------------------------------------------------------------------/

	public static String encode(byte[] sourceBytes)
	{
		int byteTriad = sourceBytes.length % 3;
		StringBuffer encoding = new StringBuffer();
		int bitOffset = 7;
		int b64Offset = 5;
		int bytePlace = 0;
		byte tokenValue = (byte)0;
		int lineLength = 0;
		while(bytePlace < sourceBytes.length)
		{
			tokenValue = (byte)((byte)tokenValue | (byte)((sourceBytes[bytePlace] & (1 << bitOffset)) > 0 ? (1 << b64Offset) : (byte)0));
			bitOffset--;
			if(bitOffset < 0)
			{
				bitOffset = 7;
				bytePlace++;
			}
			b64Offset--;
			if(b64Offset < 0)
			{
				b64Offset = 5;
				encoding.append(Base64Tokens.elementAt(tokenValue));
				tokenValue = (byte)0;
				lineLength++;
				if(lineLength > LINEMAX)
				{
					encoding.append(CARRIAGE);
					encoding.append(LINEFEED);
					lineLength = 0;
				}
			}
		}
		if(b64Offset != 5)
		{
			bytePlace--;
			for(int i = b64Offset; i >= 0; i--)
			{
				if(bitOffset >= 0)
				{
					tokenValue = (byte)((byte)tokenValue | (byte)((sourceBytes[bytePlace] & (1 << bitOffset)) > 0 ? (1 << i) : (byte)0));
				}
				bitOffset--;
			}
			encoding.append(Base64Tokens.elementAt(tokenValue));
		}	
		if(byteTriad == 2)
		{
			encoding.append(BASE64PAD);
		}
		else if(byteTriad == 1)
		{
			encoding.append(BASE64PAD);
			encoding.append(BASE64PAD);
		}
		return encoding.toString();
	}

	public static String encode(String source)
	{
		return encode(source.getBytes());
	}

	public static byte[] decodeBytes(byte[] source)
	{
		int bitOffset = 7;
		int b64Offset = 5;
		int bytePlace = 0;
		byte charValue = (byte)0;
		byte[] byteBack = new byte[source.length];
		int bytecount = 0;
		while(bytePlace < source.length)
		{
			if((char)(source[bytePlace]) == BASE64PAD)
			{
				// end processing when encountering special end-padding character
				break;
			}
			if((char)(source[bytePlace]) == LINEFEED || (char)(source[bytePlace]) == CARRIAGE)
			{
				// ignore standard line break characters
				bytePlace++;
				continue;
			}
			else
			{
				if(!Base64Tokens.contains("" + (char)(source[bytePlace])))
				{
					// ignore unknown characters (mostly implemented to deal with other line break character sequences)
					bytePlace++;
					continue;
				}
				else
				{
					byte currentByte = source[bytePlace];
					charValue = (byte)((byte)charValue | (byte)((currentByte & (1 << b64Offset)) > 0 ? (1 << bitOffset) : (byte)0));
					bitOffset--;
					if(bitOffset < 0)
					{
						bitOffset = 7;
						byteBack[bytecount] = charValue;
						bytecount++;
						charValue = (byte)0;
					}
					b64Offset--;
					if(b64Offset < 0)
					{
						b64Offset = 5;
						bytePlace++;
					}
				}
			}
		}
		return byteBack;
	}

	public static byte[] decodeBytes(String source)
	{
		return decodeBytes(source.getBytes());
	}

	public static String decode(String source)
	{
		return new String(decodeBytes(source));
	}
}