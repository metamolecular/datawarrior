/*
 * Created on Apr 15, 2004
 *
 */
package com.actelion.research.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Thread-safe Map whose key is an array of int
 * 
 * @author freyssj
 */
public class MultipleIntMap<T> {
	
	public static class IntArray {
		private final int[] array;
		public IntArray(int[] array) {
			this.array = array;
		}
		@Override
		public int hashCode() {
			int hash = 0;
			for (int i = 0; i < array.length; i++) {
				hash = (hash*32 + array[i])%Integer.MAX_VALUE;
			}
			return hash;
		}
		public int[] getArray() {
			return array;
		}
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof IntArray)) return false;
			int[] a = ((IntArray) obj).array;
			if(a.length!=array.length) return false;
			for (int i = 0; i < array.length; i++) {
				if(array[i]!=a[i]) return false;
			}
			return true;
		}
		@Override
		public String toString() {
			return Arrays.toString(array);
		}
	}
	private final int size;
	private final Map<IntArray, T> map = new ConcurrentHashMap<IntArray, T>();
	
	public MultipleIntMap(int size) {
		this.size = size;
	}
	
	public void put(final int[] array, T o) {
		if(array.length!=size) throw new IllegalArgumentException("Invalid size: "+array.length+" instead of "+size);		
		IntArray key = new IntArray(array);
		map.put(key, o);
	}
	
	public T get(final int[] array) {
		if(array.length!=size) throw new IllegalArgumentException("Invalid size: "+array.length);
		return map.get(new IntArray(array));
	}
	
	public void clear() {
		map.clear();
	}
	
	public List<int[]> keys() {
		List<int[]> keys = new ArrayList<int[]>();
		for(IntArray array: map.keySet()) {
			keys.add(array.array);
		}
		return keys;
	}
	
	
	public static void main(String[] args) {
		 MultipleIntMap<String> map  = new MultipleIntMap<String>(2);
		 map.put(new int[] {0,0}, "00");
		 map.put(new int[] {0,1}, "01");
		 map.put(new int[] {0,2}, "02");
		 map.put(new int[] {0,3}, "03");
		 map.put(new int[] {1,0}, "20");
		 map.put(new int[] {1,1}, "21");
		 System.out.println(map.get(new int[] {1,0}));
	}
}
