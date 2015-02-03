/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.util;

import java.io.*;

public class BinaryDecoder {
	private BufferedReader	mReader;
	private int				mAvailableBufferBits,mData,mDataBitCount,
							mBitsPerCharacter,mBaseCharacter;
	private int[]			mMask;

	/**
	 * Convenience method to directly decode a String-encoded byte array.
	 * @param encodedBytes
	 * @param dataBitCount count of bits per byte
	 * @return
	 */
	public static byte[] toBytes(String encodedBytes, int dataBitCount) {
		BinaryDecoder decoder = new BinaryDecoder(new BufferedReader(new StringReader(encodedBytes)));
		try {
			byte[] data = new byte[decoder.initialize(dataBitCount)];
			for (int i=0; i<data.length; i++)
				data[i] = (byte)decoder.read();
			return data;
			} catch (IOException ioe) {}
		return null;
		}

	/**
	 * Convenience method to directly decode a String-encoded int array.
	 * @param encodedBytes
	 * @param dataBitCount count of bits per byte
	 * @return
	 */
	public static int[] toInts(String encodedBytes, int dataBitCount) {
		BinaryDecoder decoder = new BinaryDecoder(new BufferedReader(new StringReader(encodedBytes)));
		try {
			int[] data = new int[decoder.initialize(dataBitCount)];
			for (int i=0; i<data.length; i++)
				data[i] = decoder.read();
			return data;
			} catch (IOException ioe) {}
		return null;
		}

	public BinaryDecoder(BufferedReader reader) {
		this(reader, 6, 64);
		}

	public BinaryDecoder(BufferedReader reader, int bitsPerCharacter, int baseCharacter) {
		mReader = reader;
		mBitsPerCharacter = bitsPerCharacter;
		mBaseCharacter = baseCharacter;
		mAvailableBufferBits = 0;

		mMask = new int[bitsPerCharacter+1];
		for (int i=1; i<=bitsPerCharacter; i++)
			mMask[i] = (mMask[i-1] << 1) | 1;
		}

	public int initialize(int dataBitCount) throws IOException {
		mDataBitCount = 32;
		int totalByteCount = read();
		mDataBitCount = dataBitCount;
		return totalByteCount;
		}

	public int read() throws IOException {
		int data = 0;
		int neededBits = mDataBitCount;
		while (neededBits != 0) {
			if (mAvailableBufferBits == 0) {
				while ((mData = mReader.read() - mBaseCharacter) < 0);
				mAvailableBufferBits = mBitsPerCharacter;
				}
			int bitsToRead = Math.min(mAvailableBufferBits, neededBits);
			int dataBits = mData & (mMask[bitsToRead] << (mAvailableBufferBits - bitsToRead));
			data |= (mAvailableBufferBits > neededBits) ?
						dataBits >> (mAvailableBufferBits - neededBits)
					  : dataBits << (neededBits - mAvailableBufferBits);
			neededBits -= bitsToRead;
			mAvailableBufferBits -= bitsToRead;
			}
		return data;
		}
	}
