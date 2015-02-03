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

package com.actelion.research.calc;

import java.util.ArrayList;

public class DataProcessor {
	private ArrayList<ProgressListener> mListener;
	private ThreadMaster	mThreadMaster;
    private boolean         mVerbose;
	
	public DataProcessor() {
		mListener = new ArrayList<ProgressListener>();
        mVerbose = true;
		}

	public void addProgressListener(ProgressListener l) {
		mListener.add(l);
		}

	public void removeProgressListener(ProgressListener l) {
		mListener.remove(l);
		}

	public void setThreadMaster(ThreadMaster t) {
		mThreadMaster = t;
		}

	public void startProgress(String message, int min, int max) {
	    if (mListener.size() == 0 && mVerbose)
			System.out.println(message);
		for (int i=0; i<mListener.size(); i++)
			mListener.get(i).startProgress(message, min, max);
		}

	public void updateProgress(int value) {
		for (int i=0; i<mListener.size(); i++)
			mListener.get(i).updateProgress(value);
		}

	public void stopProgress(String message) {
	    if (mListener.size() == 0 && mVerbose)
			System.out.println(message);
		for (int i=0; i<mListener.size(); i++)
			mListener.get(i).stopProgress();
		}

	public boolean threadMustDie() {
		return (mThreadMaster == null) ? false : mThreadMaster.threadMustDie();
		}
	
	public void setVerbose(boolean verbose) {
        mVerbose = verbose;
	    }
	}
