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

package com.actelion.research.datawarrior.task;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.table.CompoundTableModel;

public abstract class AbstractTask implements ProgressController,Runnable {
	private JDialog				mDialog;
	private TaskUIDelegate		mUIDelegate;
	private boolean				mStatusOK,mIsLive;
    private volatile Frame		mParentFrame;
	private volatile ProgressController	mProgressController;
	private volatile Properties	mTaskConfiguration;
	private volatile boolean	mUseOwnThread;

	public static String configurationToString(Properties configuration) {
	    try {
	    	Writer writer = new StringWriter();
	    	configuration.store(writer, null);
	    	return writer.toString();
	    	}
	    catch (IOException ioe) {
		    return null;
	    	}
		}

	public static Properties configurationFromString(String configurationString) {
		Properties configuration = new Properties();
		try {
			configuration.load(new StringReader(configurationString));
			return configuration;
			}
	    catch (IOException ioe) {
		    return null;
	    	}
		}

	public static String constructTaskCodeFromName(String taskName) {
		return taskName.substring(0,1).toLowerCase() + taskName.substring(1).replaceAll("[^a-zA-Z0-9]", "");
		}

	/**
	 * Checks, whether a configuration can be created that would make the task
	 * executable under current environmental conditions. A task that does some calculation
	 * on chemical structures, e.g. is not configurable, if the current data doesn't
	 * contain any column with chemical structures. If a task is not configurable,
	 * it should call showErrorMessage() with a user-understandable message.
	 * For tasks, which don't need a configuration, this method returns,
	 * whether the task is currently executable.
	 * @return true if a configuration can be set that makes the task executable
	 */
	public abstract boolean isConfigurable();

	/**
	 * Override this, if a previously recorded task of the same kind should be removed from
	 * the list of recorded task, if it becomes redundant in the light of this task being
	 * recorded now. This method is only called if the previously recorded task's class
	 * matches this task's class.
	 * <br>Example: A DETaskAssignOrZoomAxes becomes superfluous if the following
	 * task is also a DETaskAssignOrZoomAxes task with different zooming parameters.
	 * @param previousConfiguration configuration of the previously recorded task
	 * @param currentConfiguration configuration of the task about to be recorded
	 * @return
	 */
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return false;
		}

	/**
     * Returns a unique task code to identify and distinguish this task
     * from others within a task sequence file.
     * @return unique task name
     */
    public String getTaskCode() {
    	return constructTaskCodeFromName(getTaskName());
    	}

    /**
     * Returns a unique task name that serves as task identification for the user.
     * In the default implementation this is also used to construct the unique task code,
     * which serves as unique name in a task sequence file.
     * @return unique task name
     */
    public abstract String getTaskName();

    /**
     * Is supposed to check whether the given configuration is a valid one.<br>
     * If isLive==true it checks, whether the task can be executed with the current dataset.<br>
     * If isLive==true isConfigurable() was called before and returned true.<br>
     * If isLive==false it checks, whether a dataset can exist for which the task can be executed.<br>
     * In case of a problem showErrorMessage() should be called and 'false' returned.
     * @param configuration t
     * @param isLive if true then the 
     * @return true if query definition is valid
     */
    public abstract boolean isConfigurationValid(Properties configuration, boolean isLive);

	/**
	 * Performs the task with the given valid(!) configuration. The calling thread
	 * may be the event dispatcher thread or another thread, depending on how this
	 * ConfigurableTask was instantiated and whether it is executed as part of a batch sequence.
	 * If the task fails, showErrorMessage() should be called.
	 * Lengthy tasks should call startProgress() and optionally repeatedly updateProgress().
	 * Lengthy tasks also should frequently call threadMustDie() and finish gracefully if
	 * this method returned true. stopProgress() should never be called within runTask().
	 * @param configuration
	 */
	public abstract void runTask(final Properties configuration);

	/**
	 * After task execution this method is called to put the correct frame in front
	 * @return null or new created created during task processing
	 */
	public abstract DEFrame getNewFrontFrame();

	/**
	 * If a task shall not show a dialog, when being launched interactively with defineAndRun(),
	 * but must show a dialog for editing as part of a macro, then pass any interactively needed
	 * parameters to the constructor and override this method to create a valid configuration object
	 * from these parameters. When this method returns a configuration object, then no dialog is shown.<br>
	 * <i>Example</i>: DETaskDuplicateView: when interactively called, the mouse location of the popup
	 * defines which view to duplicate. Thus, showing a dialog is not necessary.
	 * @return null or valid configuration, if the task is predefined and is interactively called
	 */
	public Properties getPredefinedConfiguration() {
		return null;
		}

	/**
	 * Interactive tasks with a predefined configuration don't show a dialog and, thus, cannot be
	 * cancelled. If a predefined interactive task is cancelled through another mechanisms, then
	 * override this method and return false. Example TaskOpenFile: Instead of a configuration dialog
	 * a file dialog is shown within the getPredefinedConfiguration() method. If the file dialog
	 * is cancelled, the task should not be executed nor recorded in a macro.
	 * This method is called only, after getPredefinedConfiguration() return something != null.
	 * @param predefinedConfiguration
	 * @return
	 */
	public boolean isPredefinedStatusOK(Properties predefinedConfiguration) {
		return true;
		}

	/**
	 * Returns the currently executed task's configuration.
	 * This method only returns valid configurations after calling execute().
	 * @return
	 */
	public Properties getTaskConfiguration() {
		return mTaskConfiguration;
		}

    /**
     * Any subclass is expected to keep a static copy of the most recently interactively
     * defined and executed configuration, which is provided and requested by this superclass.
     * @return recent configuration or null
     */
	public abstract Properties getRecentConfiguration();

    /**
     * Any subclass is expected to keep a static copy of the most recently interactively
     * defined and executed configuration, which is provided and requested by this superclass.
     * @param configuration interactively used configuration
     */
	public abstract void setRecentConfiguration(Properties configuration);

	public AbstractTask(Frame owner, boolean useOwnThread) {
		mParentFrame = owner;
		mUseOwnThread = useOwnThread;
		}

	/**
	 * Provided the dialog is visible, this methods returns the configuration dialog,
	 * which is the parent of the configuration panel.
	 * @return configuration dialog
	 */
	public JDialog getDialog() {
		return mDialog;
		}

	/**
	 * Override this to use a dialog title different from the task name.
	 * @return
	 */
	public String getDialogTitle() {
		return getTaskName();
		}

	/**
	 * Override this if the task does not have any configuration items.
	 * @return
	 */
	public boolean isTaskWithoutConfiguration() {
		return false;
		}

	/**
	 * Shows a custom dialog to let the user define or update a given configuration.
	 * The dialog contains the configuration panel returned by createDialogContent()
	 * plus OK, Cancel and Help buttons. The mStatusOK flag is set depending on whether the
	 * user selects Cancel or OK.
	 * If the user cancels the dialog the given configuration is returned.
	 * If the task has no configuration items the mStatusOK flag is set to true and
	 * the given configuration returned.
	 * @param configuration to be edited or null
	 * @param isLive true if the edited configuration is used for the current table model
	 * @return updated or original configuration or null
	 */
	public Properties showDialog(Properties configuration, boolean isLive) {
		if (isTaskWithoutConfiguration()) {
			mStatusOK = true;
	        return configuration;
			}

		Properties predefinedConfiguration = getPredefinedConfiguration();
		if (predefinedConfiguration != null) {
			mStatusOK = isPredefinedStatusOK(predefinedConfiguration)
					 && isConfigurationValid(predefinedConfiguration, isLive);
	        return predefinedConfiguration;
			}

		JComponent content = getUIDelegate().createDialogContent();
	    if (content == null) {
			mStatusOK = false;
	        return configuration;
	    	}

		mDialog = new JDialog(mParentFrame, getDialogTitle(), true);
		mDialog.getContentPane().setLayout(new BorderLayout());
		mDialog.getContentPane().add(content, BorderLayout.CENTER);

		JPanel buttonPanel = createButtonPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		mDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		if (configuration != null)
			getUIDelegate().setDialogConfiguration(configuration);
		else if (mTaskConfiguration != null)
			getUIDelegate().setDialogConfiguration(mTaskConfiguration);
		else if (getRecentConfiguration() != null)
			getUIDelegate().setDialogConfiguration(getRecentConfiguration());
		else
			getUIDelegate().setDialogConfigurationToDefault();

		mIsLive = isLive;
		mDialog.pack();
		mDialog.setLocationRelativeTo(mParentFrame);
		mDialog.setVisible(true);

		return mStatusOK ? getUIDelegate().getDialogConfiguration() : configuration;
		}

	private TaskUIDelegate getUIDelegate() {
		if (mUIDelegate == null)
			mUIDelegate = createUIDelegate();

		return mUIDelegate;
		}

	/**
	 * Gets the delegate that handles dialog content creation
	 * and configuration synchronization with the UI elements.
	 * @return
	 */
	public abstract TaskUIDelegate createUIDelegate();

	/**
	 * When overriding showDialog() to suppress actually showing a dialog,
	 * then this method can be used to simulate that the OK button has been pressed
	 * and that the configuration is valid.
	 */
	public void setStatusOK() {
		mStatusOK = true;
		}

	/**
	 * Checks whether the task if fully configured to run on an external dataset.
	 * This means, that the task doesn't need a configuration or that
	 * the task dialog's OK button has been pressed without any configuration errors.
	 * @return true if the dialog was closed by pressing OK without
	 */
	public boolean isStatusOK() {
		return mStatusOK;
		}

	/**
	 * Creates the default button panel with OK and Cancel buttons on the right.
	 * A Help button is shown in addition, if getHelpURL() != null.
	 * Override this if more buttons are needed. Make this class an ActionListener
	 * of OK and Cancel buttons and make the OK button the default button of the
	 * getDialog()s root pane. 
	 * @return
	 */
	private JPanel createButtonPanel() {
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("Help")) {
				    final JEditorPane helpPane = new JEditorPane();
					helpPane.setEditorKit(new HTMLEditorKit());
					helpPane.setEditable(false);
				    helpPane.addHyperlinkListener(new HyperlinkListener() {
				    	public void hyperlinkUpdate(HyperlinkEvent e) {
				    		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				    			URL url = e.getURL();
				    			try {
				    				helpPane.setPage(url);
				    				}
				    			catch(IOException ioe) {}
				    			}
				    		}
				    	});

					URL url = createURL(getHelpURL());
					if (url != null)
						try { helpPane.setPage(url); } catch (IOException ioe) {}

					JDialog helpDialog = new JDialog(getDialog(), "Help "+getTaskName(), false);
					helpDialog.setSize(720, 500);
					helpDialog.getContentPane().add(new JScrollPane(helpPane,
					        		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					        		JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
					helpDialog.setVisible(true);
					return;
					}

				mStatusOK = false;
				if (e.getActionCommand().equals("Cancel"))
					doCancelAction();

				if (e.getActionCommand().equals("OK"))
					doOKAction();
				}
			};

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		JButton[] optionButtons = getAccessoryButtons();
		if (optionButtons != null || getHelpURL() != null) {
			JPanel libp = new JPanel();
			int count = (optionButtons == null ? 0 : optionButtons.length) + (getHelpURL() == null ? 0 : 1);
			libp.setLayout(new GridLayout(1, count, 8, 0));
			if (getHelpURL() != null) {
				JButton bhelp = new JButton("Help");
				bhelp.addActionListener(al);
				libp.add(bhelp);
				}
			if (optionButtons != null)
				for (JButton b:optionButtons)
					libp.add(b);
			buttonPanel.add(libp, BorderLayout.WEST);
			}
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(al);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(al);
		ibp.add(bok);
		buttonPanel.add(ibp, BorderLayout.EAST);
		mDialog.getRootPane().setDefaultButton(bok);
		return buttonPanel;
		}

	/**
	 * Keeps the mStatusOK flag on false and closes the dialog.
	 */
	public void doCancelAction() {
		closeDialog();
		}

	/**
	 * If the dialog configuration is valid, then sets the mStatusOK flag to true and closes the dialog.
	 * Setting mStatusOK to true causes defineAndRun() to actually run the task.
	 * If you override this method, then make sure to call setStatusOK() or otherwise make sure
	 * that the pressing OK and Cancel work in an expected way.
	 */
	public void doOKAction() {
		if (isConfigurationValid(getUIDelegate().getDialogConfiguration(), mIsLive)) {
			mStatusOK = true;
			closeDialog();
			}
		}

	/**
	 * Closes the dialog.
	 */
	public void closeDialog() {
		mDialog.setVisible(false);
		mDialog.dispose();
		mDialog = null;
		}

	public void setOKButtonEnabled(boolean b) {
		mDialog.getRootPane().getDefaultButton().setEnabled(b);
		}

	/**
	 * Override this to add buttons into the bottom left corner of the dialog.
	 * If a help button is shown, the accessory buttons will show up at its right side.
	 * All buttons (including help) will be equally sized.
	 * @return additional buttons to be shown at the bottom of the dialog
	 */
	public JButton[] getAccessoryButtons() {
		return null;
		}

	/**
	 * Override this to show a help button left of Cancel and OK,
	 * which shows a help window with the content of the given URL when pressed.
	 * @return whether a help button shall be displayed
	 */
	public String getHelpURL() {
		return null;
		}

	private URL createURL(String urlText) {
    	String ref = null;
    	int index = urlText.indexOf('#');
    	if (index != -1) {
    		ref = urlText.substring(index);
    		urlText = urlText.substring(0, index);
    		}
    	URL theURL = getClass().getResource(urlText);
    	if (ref != null) {
    		try {
    			theURL = new URL(theURL, ref);
    			}
    		catch (IOException e) {
    			return null;
    			}
			}
		return theURL;
		}

    public final Frame getParentFrame() {
    	return mParentFrame;
    	}

    public void startProgress(String text, int min, int max) {
    	if (mProgressController != null)
    		mProgressController.startProgress(text, min, max);
    	}

	public void updateProgress(int value) {
    	if (mProgressController != null)
    		mProgressController.updateProgress(value);
		}

	public void stopProgress() {
		// Don't let any employed class call stopProgress on mProgressController.
		// mProgressController.stopProgress() is called explicitly from execute()
		// after the ConfigurableTask has finished.
		}

	/**
	 * This should be used during task validity checking and task execution.
	 * @param message
	 */
	public void showErrorMessage(String message) {
		if (mProgressController == null)
			showInteractiveTaskMessage(message, JOptionPane.ERROR_MESSAGE);
		else
			mProgressController.showErrorMessage("Error in task '"+getTaskName()+"':\n"+message);
		}

	public boolean threadMustDie() {
		return (mProgressController == null) ? false : mProgressController.threadMustDie();
		}

	/**
	 * Interactively let the user configure the task and run it.
	 * This method must be called from the EventDispatchThread.
	 * Non-configurable tasks are executed without showing a dialog.
	 * If configuring the task is not possible a message is shown.
	 * In this case or if the user selects <Cancel> no task is performed.
	 * If the user clicks OK and the configuration is invalid, a message is
	 * displayed and the dialog is kept open.
	 */
    public void defineAndRun() {
    	if (isConfigurable()) {
	    	Properties configuration = showDialog(getRecentConfiguration(), true);
	    	if (mStatusOK) {	// the configuration is valid
	    		setRecentConfiguration(configuration);

	    		if (!(this instanceof GenericTaskRunMacro))
	    			DEMacroRecorder.record(this, configuration);
	    		
    			mTaskConfiguration = configuration;
    			if (mUseOwnThread) {
    		        mProgressController = new JProgressDialog(mParentFrame, true);
    		        Thread t = new Thread(this, getTaskCode());
    		        t.setPriority(Thread.MIN_PRIORITY);
    		        t.start();
    				}
    			else {
    				runTask(configuration);
    				}
	    		}
    		}
    	}

    /**
     * Provided the task is configurable and the given configuration is valid,
     * this method runs the task. This method must not be called from the EventDispatchThread.
     * This method is called when a sequence of predefined tasks is executed.
     * For interactively running one task use defineAndRun().
     * @param configuration
     * @param pc
     */
	public void execute(final Properties configuration, final ProgressController pc) {
		mProgressController = pc;
    	if (isConfigurable()
    	 && isConfigurationValid(configuration, true)) {
			mTaskConfiguration = configuration;

    		if (!(this instanceof GenericTaskRunMacro))
    			DEMacroRecorder.record(this, configuration);

			if (!mUseOwnThread) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							runTask(configuration);
							}
						} );
					}
				catch (Exception e) {}
				}
			else {
				runTask(configuration);
				}
			}
		}

	/**
	 * This returns the current ProgressController.
	 * @return progress controller or null if not executing currently
	 */
	public ProgressController getProgressController() {
		return mProgressController;
		}

	/**
	 * 
	 */
	@Override
	public void run() {
		try {
			runTask(mTaskConfiguration);
			}
		catch (OutOfMemoryError e) {
			showErrorMessage("Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.");
			}

		mProgressController.stopProgress();
		((JProgressDialog)mProgressController).close(getNewFrontFrame());
    	mProgressController = null;
		}

	/**
	 * Creates a TAB delimited String containing no-alias-column-names of the
	 * columns being selected in the list.
	 * @param list
	 * @param tableModel
	 * @return null or TAB delimited column name list
	 */
	public String getSelectedColumnsFromList(JList list, CompoundTableModel tableModel) {
		StringBuilder sb = null;
		for (Object item:list.getSelectedValues()) {
			int column = tableModel.findColumn((String)item);
			String columnName = (tableModel.getParentColumn(column) == -1) ?
					tableModel.getColumnTitleNoAlias(column) : (String)item;
			if (sb == null) {
				sb = new StringBuilder(columnName);
				}
			else {
				sb.append('\t');
				sb.append(columnName);
				}
			}
		return (sb == null) ? null : sb.toString();
		}

	/**
	 * Selects all items (column titles) in a list that are referenced in a tab delimited string
	 * @param list
	 * @param columnNames TAB delimited column names recognizable by tableModel
	 * @param tableModel
	 */
	public void selectColumnsInList(JList list, String columnNames, CompoundTableModel tableModel) {
    	list.clearSelection();
    	String columnList = columnNames;
    	if (columnList != null) {
    		for (String columnName:columnList.split("\\t")) {
    			int column = tableModel.findColumn(columnName);
    			if (column != -1) {
    				String nameInList = tableModel.getColumnTitle(column);
    		    	for (int i=0; i<list.getModel().getSize(); i++) {
    		    		if (nameInList.equals(list.getModel().getElementAt(i))) {
    		    			list.addSelectionInterval(i, i);
    		    			break;
    		    			}
    		    		}
    				}
    			}
    		}
		}

	/**
	 * Tries to find item in itemList. If successful it returns the list index.
	 * If item is null or item is not found, defaultIndex is returned.
	 * @param item
	 * @param itemList
	 * @param defaultIndex
	 * @return
	 */
    public static int findListIndex(String item, String[] itemList, int defaultIndex) {
    	if (item != null)
    		for (int i=0; i<itemList.length; i++)
    			if (item.equals(itemList[i]))
    				return i;
    	return defaultIndex;
    	}

    /**
     * Checks whether the file (and path) are valid and in case of saving,
     * whether the parent directory is existing and permissions are OK.
     * If there is a problem, showErrorMessage() is called and false is returned.
     * @param filename complete path and file name as from File.getPath()
     * @param isSaving true if save, false if open
     * @param askOverwrite if true and if isSaving and if the file exists the user is asked whether to replace the existing file
     * @return true if file is readable (isSaving==false) / can be written (isSaving==true)
     */
    public boolean isFileAndPathValid(String filename, boolean isSaving, boolean askOverwrite) {
		if (filename == null || filename.length() == 0) {
			showErrorMessage("No file name specified.");
			return false;
			}
		File file = new File(resolveVariables(filename));
		if (!file.exists()) {
			if (!isSaving) {
				showErrorMessage("File not found:\n"+filename);
				return false;
				}
			File parent = file.getParentFile();
			if (parent == null) {
				showErrorMessage("Parent directory not defined:\n"+filename);
				return false;
				}
			if (!parent.exists()) {
				showErrorMessage("Directory does not exist:\n"+parent.getPath());
				return false;
				}
			try {
				// Create and delete a dummy file in order to check file permissions.
				file.createNewFile();
			    file.delete();
				}
			catch(IOException e) {
				showErrorMessage("No privileges to write in directory.");
				return false;
				}
			}
		else {
			if (isSaving) {
				if (!file.canWrite()) {
					showErrorMessage("No privileges to overwrite file:\n"+filename);
					return false;
					}
				if (askOverwrite && JOptionPane.showConfirmDialog(mParentFrame,
						"A file with this name already exists.\nDo you want to replace the existing file?", "Warning",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION) {
					return false;
					}
				}
			else {
				if (!file.canRead()) {
					showErrorMessage("No privileges to read file:\n"+filename);
					return false;
					}
				}
			}
		return true;
    	}

	public String resolveVariables(String path) {
		return DataWarrior.resolveVariables(path);
		}

	/**
	 * This wait for the completion of the descriptor calculation.
	 * This method should be called when executing a macro task that
	 * requires a specific descriptor to be present.
	 * @param tableModel
	 * @param column
	 */
	public void waitForDescriptor(CompoundTableModel tableModel, int column) {
    	if (!tableModel.isDescriptorAvailable(column)) {
			startProgress("Waiting descriptor calculation: "+tableModel.getColumnTitle(column)+"'...", 0, 0);
			while (!threadMustDie() && !tableModel.isDescriptorAvailable(column)) {
				try {
					Thread.sleep(100);
					if (threadMustDie())
						break;
					}
				catch (InterruptedException e) {}
				}
    		}
		}

	/**
	 * This shows an interactive message dialog and may be used after
	 * interactive task execution, if the task finished successfully and
	 * the user needs to notified. If a task is running within a macro, don't
	 * use this method. If an error occurs, use showErrorMessage().<br>
	 * This method can be called from any thread.
	 * @param message
	 */
	public void showInteractiveTaskMessage(final String message, final int messageType) {
		if (SwingUtilities.isEventDispatchThread()) {
			JOptionPane.showMessageDialog(mParentFrame, message, getTaskName(), messageType);
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message, getTaskName(), messageType);
					}
				} );
			}
		}
	}
