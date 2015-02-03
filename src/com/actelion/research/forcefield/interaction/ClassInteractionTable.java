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
 * @author Joel Freyss
 */
package com.actelion.research.forcefield.interaction;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;


/**
 * 
 * @author freyssj
 */
public class ClassInteractionTable {
	
	private static ClassInteractionTable instance = null; 
	private final ClassInteractionStatistics stats;
	private final InteractionDescriptor[][] clazIds2Descriptor;
	private final double[][] similarityTable;
	

	/**
	 * 
	 * 
	 */
	public static class InteractionDescriptor {
		public int N;
		public double optimalDist;
		public double strength;
		
		public InteractionDescriptor(PLFunction plf) {
			if(plf==null) return;
			N = plf.getTotalOccurences();
			try {
				//Find the minima of this function
				strength = 100;
				optimalDist = -1;
				for (double d = 1.6; d < 4.5; d+=.05) {
					double v = plf.getFGValue(d)[0];
					if(v<strength) {
						optimalDist = d;
						strength = v;
					}
				}							
			} catch (Exception e) {
				e.printStackTrace();
			}			 
		}
		
		public double dist(InteractionDescriptor d2) {
			
			double s1 = optimalDist>0?strength:0;
			double m1 = optimalDist>0?optimalDist:4.5;
			double s2 = d2.optimalDist>0?d2.strength:0;
			double m2 = d2.optimalDist>0?d2.optimalDist:4.5;
			
			return (m1-m2)*(m1-m2) + (s1-s2)*(s1-s2)/9;
			 
		}
				
		@Override
		public String toString() {return new DecimalFormat("0.0").format(optimalDist)+":"+new DecimalFormat("0.00").format(strength)+" ["+N+"]";}

		
	}
		
	/**
	 * Private Constructor (singleton pattern)
	 * Creates a table of similarity between the interaction atom types
	 *  
	 *
	 */
	private ClassInteractionTable() {
		stats = ClassInteractionStatistics.getInstance(false);

		int N = stats.getNClasses();
		
		//Prepare the proteinLigandIDs table
		clazIds2Descriptor = new InteractionDescriptor[N][N];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				PLFunction plf = stats.getFunction(i, j);
				clazIds2Descriptor[i][j] = new InteractionDescriptor(plf);
			}			
		}
		
		//Create the similarityTable table
		similarityTable = new double[N][N];
		for (int l1 = 0; l1 < N; l1++) {
			for (int l2 = l1; l2 < N; l2++) {
				double sum = 0;
				int total = 0;
				for (int i = 0; i < clazIds2Descriptor.length; i++) {
					InteractionDescriptor id1 = getInteractionDescriptor(i, l1); 
					InteractionDescriptor id2 = getInteractionDescriptor(i, l2);
					double coeff = id1.N+id2.N;
					sum   += id1.dist(id2) * coeff;
					total += coeff; 
				}
				similarityTable[l1][l2] = similarityTable[l2][l1] = total>0? sum/total: 5;		
			}			
		}
	}
	
	public static ClassInteractionTable getInstance() {
		if(instance==null) instance = new ClassInteractionTable();
		return instance;
	}
	
	public InteractionDescriptor getInteractionDescriptor(int i, int j) {
		return clazIds2Descriptor[i][j];
	}
	
	
	/**
	 * D(LigandType_1, LigandType_2) = Sum( d( F(ProteinType_i, LigandType_1), F(ProteinType_i, LigandType_2)), i) 
	 * @param l1
	 * @param l2
	 * @return
	 */
	
	public double getDistance(int l1, int l2) {
		return similarityTable[l1][l2];
	}
	
	
	public void printTable() {
		List<String> allTypes = new ArrayList<String>();
		for (int i = 0; i < similarityTable.length; i++) {
			String d = stats.getDescription(i);
			if(!d.startsWith("6*") && !d.startsWith("7*") && !d.startsWith("8*")) continue;
			allTypes.add(d);
		}
		Collections.sort(allTypes);
		
		DecimalFormat df = new DecimalFormat("0.00");
		
		System.out.print("\t");
		int count = 0;
		for (String t1 : allTypes) {
			System.out.print((++count)+". " + t1+"\t");			
		}			
		System.out.println();
		
		count = 0;
		for (String t1 : allTypes) {
			System.out.print((++count)+". " + t1+"\t");
			for (String t2 : allTypes) {
				double d = 1-getDistance(stats.getClassId(t1), stats.getClassId(t2));
				if(d<0) d=0;
				System.out.print(df.format(d)+"\t");
			}
			System.out.println();
		}		
	}
	
	public void printEquivalents() {
		Set<Integer> seen = new HashSet<Integer>();
		int n = 0;
		for (int type = 0; type < similarityTable.length; type++) {
			if(seen.contains(type)) continue;
			if(stats.getParent(type)==type) continue;
			String d = stats.getDescription(type);
			if(!d.startsWith("6*") && !d.startsWith("7*") && !d.startsWith("8*")) continue;
			
			
			List<Integer> list = getEquivalentTypes(type, .2);
			if(list.size()==0) continue; //Incomplete interactions
			
			System.out.println("Cluster "+(++n)+" ["+type+"] "+ stats.getDescription(type));

			for (Integer t2 : list) {
				if(seen.contains(t2)) continue;
				String d2 = stats.getDescription(t2);
				if(!d2.startsWith("6*") && !d2.startsWith("7*") && !d2.startsWith("8*")) continue;
				
				System.out.println(" -> ["+t2+"] " + stats.getDescription(t2)+ " > "+getEquivalence(type, t2));
				seen.add(t2);
			}
			
			System.out.println();
			seen.add(type);
			
		}
	}

	public double getEquivalence(int type1, int type2) {
//		return getDistance(type1, type2);
		double sum = 0;
		for (int i = 0; i < similarityTable.length; i++) {
			double diff = Math.abs( getDistance(type1, i) - getDistance(type2, i));
			sum+=diff;
		}
		return sum/ similarityTable.length;
	}
	/**
	 * Compare similarity values of 2 types (across all lines)
	 * @param type1
	 * @param maxAvg
	 * @return
	 */
	public List<Integer> getEquivalentTypes(int type, double maxDist) {
		List<Integer> res = new ArrayList<Integer>();
		for (int type2 = 0; type2 < similarityTable.length; type2++) {
			if(stats.getParent(type2)==type2) continue;
			
			if(getEquivalence(type, type2)<maxDist) {
				res.add(type2);
			}
		}
		return res;		
	}
	
	
	/**
	 * EXAMPLE
	 * @param args
	 */
	public static void main(String[] args) {
		final ClassInteractionTable c = new ClassInteractionTable();
		
		
		final DefaultListModel model1 = new DefaultListModel();
		final JList list1 = new JList(model1);		
		final DefaultTableModel model2 = new DefaultTableModel();
		final JTable list2 = new JTable(model2);
		final JCheckBox onlyMainTypes = new JCheckBox("Only Main Types");
		onlyMainTypes.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				model1.clear();
				for (int i= 0; i<c.stats.getNClasses();i++) {
					if(onlyMainTypes.isSelected() && c.stats.getParent(i)!=i) continue;
					if(!onlyMainTypes.isSelected() && c.stats.getParent(i)==i) continue;
					if(!c.stats.getDescription(i).startsWith("6*") && !c.stats.getDescription(i).startsWith("7*") && !c.stats.getDescription(i).startsWith("8*") && !c.stats.getDescription(i).startsWith("15*") && !c.stats.getDescription(i).startsWith("16*")) continue;
					model1.addElement("["+i+"] " + c.stats.getDescription(i));
				}
				System.out.println("ClassInteractionTable.main() types.size()="+model1.size());
			}
		});
		onlyMainTypes.getActionListeners()[0].actionPerformed(null);

		list1.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				Object[] objs = list1.getSelectedValues();
				int sel1 = -1;
				
				List<Integer> sels = new ArrayList<Integer>();
				if(objs.length>0) {
					String s1 = (String) objs[0];
					s1 = s1.substring(s1.indexOf("] ")+2);
					sel1 = c.stats.getClassId(s1);
				}
				for (int i = 0; i < objs.length; i++) {					
					String s2 = (String) objs[i];
					s2 = s2.substring(s2.indexOf("] ")+2);
					sels.add(c.stats.getClassId(s2));
				}
				
				
				com.actelion.research.util.PriorityQueue<Integer> queue = new com.actelion.research.util.PriorityQueue<Integer>();
				if(sel1>=0) {
					for (int i = 0; i<c.stats.getNClasses();i++) {
						if(onlyMainTypes.isSelected() && c.stats.getParent(i)!=i) continue;
						if(!onlyMainTypes.isSelected() && c.stats.getParent(i)==i) continue;
						if(sels.size()>1 && !sels.contains(i)) continue;
						if(!c.stats.getDescription(i).startsWith("6*") && !c.stats.getDescription(i).startsWith("7*") && !c.stats.getDescription(i).startsWith("8*") && !c.stats.getDescription(i).startsWith("15*") && !c.stats.getDescription(i).startsWith("16*")) continue;
						queue.add(i, c.getEquivalence(sel1, i));
					}
				}
			
				Object[][] data = new Object[c.stats.getNClasses()][];
				for (int i = 0; i < queue.size(); i++) {
					data[i] = new Object[] { "["+i+"] " + c.stats.getDescription(queue.getElt(i).obj),
							new DecimalFormat("0.00").format(c.getEquivalence(sel1, queue.getElt(i).obj)), 
							new DecimalFormat("0.00").format(c.getDistance(sel1, queue.getElt(i).obj))}; 
				}
				
				model2.setDataVector(data, new String[] {"Type", "Equivalence", "Distance" });
			}
		});
		
		JSplitPane panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list1), new JScrollPane(list2));

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, panel);
		contentPane.add(BorderLayout.SOUTH, onlyMainTypes);
		
		
		JFrame frame = new JFrame("Table similarities");
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		
	}
	
	
	
}
