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

package com.actelion.research.datawarrior;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.DETaskSelectWindow;
import com.actelion.research.datawarrior.task.StandardTaskFactory;
import com.actelion.research.datawarrior.task.file.DETaskOpenFile;
import com.actelion.research.datawarrior.task.file.DETaskRunMacroFromFile;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableDetailHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.util.Platform;

public class DataWarrior implements WindowFocusListener {
	public static final String PROGRAM_NAME = "DataWarrior";
	public static final String PREFERENCES_ROOT = "org.openmolecules.datawarrior";
	public static final String PREFERENCES_KEY_FIRST_LAUNCH = "first_launch";
	public static final String PREFERENCES_KEY_AUTO_UPDATE_CHECK = "automatic_update_check";

	private ArrayList<DEFrame>	mFrameList;
	private DEFrame				mFrameOnFocus;
	private StandardTaskFactory	mTaskFactory;

	public static String resolveVariables(String path) {
		if (path != null && path.toLowerCase().startsWith("$home")) {
			String home = System.getProperty("user.home");
			String rest = path.substring(5);
			return home.concat(Platform.isWindows() ? rest.replace('/', '\\') : rest.replace('\\', '/'));
			}
		return path;
		}

	public DataWarrior() {
		mFrameList = new ArrayList<DEFrame>();
		createNewFrame(null, false);
		new DEAboutDialog(mFrameOnFocus, 2000);

		initialize();

		if (!isActelion()) {
			try {
				Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);

				long firstLaunchMillis = prefs.getLong(PREFERENCES_KEY_FIRST_LAUNCH, 0L);
				if (firstLaunchMillis == 0L) {
					prefs.putLong(PREFERENCES_KEY_FIRST_LAUNCH, System.currentTimeMillis());
					}

				if (prefs.getBoolean(PREFERENCES_KEY_AUTO_UPDATE_CHECK, true))
					checkVersion(false);
				}
			catch (Exception e) {}
			}

		mTaskFactory = createTaskFactory();
		}

	public StandardTaskFactory createTaskFactory() {
		return new StandardTaskFactory();
		}

	public DEDetailPane createDetailPane(CompoundTableModel tableModel) {
		return new DEDetailPane(tableModel);
		}

	public CompoundTableDetailHandler createDetailHandler(Frame parent, CompoundTableModel tableModel) {
		return new CompoundTableDetailHandler(tableModel);
		}

	public void initialize() {
		}

	public void checkVersion(boolean showUpToDateMessage) {
		DEVersionChecker.checkVersion(mFrameOnFocus, showUpToDateMessage);
		}

	public StandardMenuBar createMenuBar(DEFrame frame) {
		return new StandardMenuBar(frame);
		}

	public DatabaseActions createDatabaseActions(DEFrame parent) {
		return null;
		}

	public boolean isActelion() {
		return false;
		}

	public StandardTaskFactory getTaskFactory() {
		return mTaskFactory;
		}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		for (DEFrame f:mFrameList) {
			if (f == e.getSource()
			 && mFrameOnFocus != f) {	// if mFrameOnFocus==e.getSource() then the frame was just created or a dialog was closed

				// we try to identify those changes, which are interactively caused by the user
				if (mFrameOnFocus != null	// if mFrameOnFocus==null then a frame was closed
				 && e.getOppositeWindow() instanceof DEFrame) {
					if (DEMacroRecorder.getInstance().isRecording()) {
						DETaskSelectWindow task = new DETaskSelectWindow(f, this, f);
						DEMacroRecorder.record(task, task.getPredefinedConfiguration());
						}
					}

				mFrameOnFocus = f;
				}
			}
		}

	@Override
	public void windowLostFocus(WindowEvent e) {}

	/**
	 * Creates a new DEFrame as front window that is expected to be populated with data immediately.
	 * This method can be called safely from any thread. If a modal dialog, e.g. a progress
	 * dialog is visible during the call of this method, then moving the new DEFrame to
	 * the front fails. In this case toFront() must be called on this frame after the
	 * dialog has been closed.
	 * The DEFrame returned has its CompoundTable lock set to indicate that the frame is
	 * about to be filled. When adding content fails for any reason, then this lock must
	 * be released with tableModel.unlock() to make the frame again available for other purposes.
	 * @param title use null for default title
	 * @return empty DEFrame to be populated
	 */
	public DEFrame getEmptyFrame(final String title) {
		for (DEFrame f:mFrameList)
			if (f.getMainFrame().getTableModel().isEmpty()
			 && f.getMainFrame().getTableModel().lock()) {
				f.setTitle(title);
				return f;
				}

		if (SwingUtilities.isEventDispatchThread()) {
			createNewFrame(title, true);
			}
		else {
				// if we are not in the event dispatcher thread we need to use invokeAndWait
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						createNewFrame(title,  true);
						}
					} );
				}
			catch (Exception e) {}
			}

		return mFrameOnFocus;
		}

	public void closeApplication() {
		while (mFrameList.size() != 0) {
			DEFrame frame = getActiveFrame();
			if (!safelyDisposeFrame(frame))
				return;
			}

		System.exit(0);
		}

	public void closeFrame(DEFrame frame) {
		safelyDisposeFrame(frame);

		if (!isMacintosh() && mFrameList.size() == 0)
			System.exit(0);
		}

	public void closeAllFrames() {
		while (mFrameList.size() != 0) {
			DEFrame frame = getActiveFrame();
			if (!safelyDisposeFrame(frame))
				return;
			}

		if (!isMacintosh())
			System.exit(0);
		}

	private boolean safelyDisposeFrame(DEFrame frame) {
		if (frame.askStopRecordingMacro()
		 && frame.askSaveDataIfDirty()) {
			mFrameList.remove(frame);
			frame.getTableModel().initializeTable(0, 0);
			frame.setVisible(false);
			frame.dispose();
			if (mFrameOnFocus == frame)
				mFrameOnFocus = null;
			return true;
			}
		return false;
		}

	public boolean isMacintosh() {
		return false;	// default
		}

	/**
	 * Opens the file, runs the query, starts the macro depending on the file type.
	 * @param filename
	 * @return new frame or null if no frame was opened
	 *
	public DEFrame readFile(String filename) {
		final int filetype = FileHelper.getFileType(filename);
		switch (filetype) {
		case FileHelper.cFileTypeDataWarrior:
		case FileHelper.cFileTypeSD:
		case FileHelper.cFileTypeTextTabDelimited:
		case FileHelper.cFileTypeTextCommaSeparated:
			final DEFrame _emptyFrame = getEmptyFrame(filename);
			new CompoundTableLoader(_emptyFrame, _emptyFrame.getTableModel()) {
				public void finalStatus(boolean success) {
					if (success && filetype == FileHelper.cFileTypeDataWarrior)
						_emptyFrame.setDirty(false);
					}
				}.readFile(new File(filename), new DERuntimeProperties(_emptyFrame.getMainFrame()), filetype);

			return _emptyFrame;
		case FileHelper.cFileTypeDataWarriorQuery:
			new DEFileLoader(getActiveFrame(), this).openAndRunQuery(new File(filename));
			return null;
		case FileHelper.cFileTypeDataWarriorMacro:
			new DEFileLoader(getActiveFrame(), this).openAndRunMacro(new File(filename));
			return null;
		default:
			JOptionPane.showMessageDialog(getActiveFrame(), "Unsupported file type.\n"+filename);
			return null;
			}
		}	*/

	/**
	 * Opens the file, runs the query, starts the macro depending on the file type.
	 * @param filename
	 */
	public void readFile(String filename) {
		final int filetype = FileHelper.getFileType(filename);
		switch (filetype) {
		case FileHelper.cFileTypeDataWarrior:
		case FileHelper.cFileTypeSD:
		case FileHelper.cFileTypeTextTabDelimited:
		case FileHelper.cFileTypeTextCommaSeparated:
		    new DETaskOpenFile(this, filename).defineAndRun();
			return;
		case FileHelper.cFileTypeDataWarriorMacro:
			new DETaskRunMacroFromFile(this, filename).defineAndRun();
			return;
		default:
			JOptionPane.showMessageDialog(getActiveFrame(), "Unsupported file type.\n"+filename);
			return;
			}
		}

	public ArrayList<DEFrame> getFrameList() {
		return mFrameList;
		}

	public DEFrame getActiveFrame() {
		for (DEFrame f:mFrameList)
			if (f == mFrameOnFocus)
				return f;

		return mFrameList.get(0);
		}

	/**
	 * If not called from the event dispatch thread and if called after closeFrame()
	 * then this call waits until this class receives a windowGainedFocus() and
	 * returns the frame that has gotten the focus. If no frames are left after
	 * one was closed, then null is returned. 
	 * @return active frame or null
	 */
	public DEFrame getNewFrontFrameAfterClosing() {
		if (mFrameList.size() == 0)
			return null;

		if (!SwingUtilities.isEventDispatchThread()) {
			while (mFrameOnFocus == null)
				try { Thread.sleep(100); } catch (InterruptedException ie) {}
			}

		return mFrameOnFocus;
		}

	/**
	 * Select another frame mimicking the user selecting the window interactively:
	 * If a macro is recording, this will cause a SelectWindow task to be recorded.
	 * @param frame
	 */
	public void setActiveFrame(DEFrame frame) {
		if (frame != mFrameOnFocus) {
			frame.toFront();

			// mFrameOnFocus = frame; Don't do this, to mimic user interaction; mFrameOnFocus will be updated through windowGainedFocus() call
			}
		}

	private void createNewFrame(String title, boolean lockForImmediateUsage) {
		DEFrame f = new DEFrame(this, title, lockForImmediateUsage);
		f.validate();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = f.getSize();
		int surplus = Math.min(screenSize.width-frameSize.width,
							   screenSize.height-frameSize.height);
		int steps = (surplus < 128) ? 8 : surplus / 16;
		int block = mFrameList.size() / steps;
		int index = mFrameList.size() % steps;

		mFrameList.add(f);
		mFrameOnFocus = f;

		f.setLocation(16 * index + 64 * block, 22 + 16 * index);
		f.setVisible(true);
		f.toFront();
		f.addWindowFocusListener(this);
		}

	/**
	 * Tries to return the directory of the datawarrior.jar file and returns its absolute path.
	 * @return empty String if DataWarrior was not launched from .jar file in file system.
	 */
	public static String getApplicationFolder() {
		try {
			CodeSource cs = DataWarrior.class.getProtectionDomain().getCodeSource();
			if (cs != null) {
				File file = new File(cs.getLocation().toURI());
				if (file.getName().endsWith(".jar"))	// on Windows this gets a file from the cache
					file.getParentFile().getAbsolutePath();
				}
			}
		catch (Exception e) {}
		return "";
		}
	}