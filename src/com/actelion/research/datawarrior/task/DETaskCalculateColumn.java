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

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.nfunk.jep.JEP;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.jep.JEPChemSimilarityFunction;
import com.actelion.research.datawarrior.task.jep.JEPContainsFunction;
import com.actelion.research.datawarrior.task.jep.JEPFrequencyFunction;
import com.actelion.research.datawarrior.task.jep.JEPIntFunction;
import com.actelion.research.datawarrior.task.jep.JEPLenFunction;
import com.actelion.research.datawarrior.task.jep.JEPLigEffFunction;
import com.actelion.research.datawarrior.task.jep.JEPParameter;
import com.actelion.research.datawarrior.task.jep.JEPRateHTSFunction;
import com.actelion.research.datawarrior.task.jep.JEPRefValueOfCategoryFunction;
import com.actelion.research.datawarrior.task.jep.JEPRoundFunction;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;

public class DETaskCalculateColumn extends ConfigurableTask
		    implements ActionListener,Runnable {
    static final long serialVersionUID = 0x20061004;

	public static final String TASK_NAME = "Calculate New Column";

	private static final String PROPERTY_FORMULA = "formula";
	private static final String PROPERTY_COLUMN_NAME = "columnName";

	private static final String PSEUDO_FUNCTION_ASK_COLUMN_VAR = "askColumnVar(";
	private static final String PSEUDO_FUNCTION_ASK_COLUMN_TITLE = "askColumnTitle(";
	private static final String PSEUDO_FUNCTION_ASK_STRING = "askString(";
	private static final String PSEUDO_FUNCTION_ASK_NUMBER = "askNumber(";
	
    private static final String IS_VISIBLE_ROW = "isVisibleRow";
    private static final String IS_SELECTED_ROW = "isSelectedRow";
    private static final String IS_MEMBER_OF = "isMemberOf_";

    private static Properties sRecentConfiguration;

	private volatile CompoundTableModel	mTableModel;
	private volatile String		mResolvedValue;
	private int					mCurrentRow;
	private JComboBox			mComboBoxColumnName;
	private JTextField          mTextFieldFormula,mTextFieldColumnName;
	private JEP					mParser;
	private TreeMap<String,Integer> mRunTimeColumnMap;

	public DETaskCalculateColumn(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
    	}

	@Override
	public JComponent createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.FILL, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.FILL, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));

		mp.add(new JLabel("Please enter a formula:"), "1,1,7,1");

		mTextFieldFormula = new JTextField(32);
		mp.add(mTextFieldFormula, "1,3,7,3");
		
		JButton buttonAdd = new JButton("Add Variable");
		buttonAdd.addActionListener(this);
		mp.add(buttonAdd, "3,5");

		TreeMap<String,Integer> columnMap = createColumnMap();

		mComboBoxColumnName = new JComboBox();
		for (String varName:columnMap.keySet())
            if (!mTableModel.isDescriptorColumn(columnMap.get(varName).intValue()))
                mComboBoxColumnName.addItem(varName);
        for (String varName:columnMap.keySet())
            if (mTableModel.isDescriptorColumn(columnMap.get(varName).intValue()))
                mComboBoxColumnName.addItem(varName);

	    mComboBoxColumnName.addItem(IS_VISIBLE_ROW);
	    mComboBoxColumnName.addItem(IS_SELECTED_ROW);

	    mp.add(mComboBoxColumnName, "5,5");

		mp.add(new JLabel("New column name:", JLabel.RIGHT), "1,7,3,7");

		mTextFieldColumnName = new JTextField();
		mp.add(mTextFieldColumnName, "5,7,7,7");

		return mp;
	    }

	@Override
	public String getHelpURL() {
		return "/html/help/analysis.html#JEP";
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Add Variable")) {
		    int index = mTextFieldFormula.getCaretPosition();
		    String text = mTextFieldFormula.getText();
		    String name = (String)mComboBoxColumnName.getSelectedItem();
		    if (index == -1)
		        mTextFieldFormula.setText(text+name);
		    else
		        mTextFieldFormula.setText(text.substring(0, index)+name+text.substring(index));
		    return;
			}
		}

	private TreeMap<String,Integer> createColumnMap() {
		TreeSet<String> varNameList = new TreeSet<String>();
		varNameList.add(IS_VISIBLE_ROW);
		varNameList.add(IS_SELECTED_ROW);

		TreeMap<String,Integer> columnMap = new TreeMap<String,Integer>();
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.isColumnDisplayable(column)) {
                String varName = removeConflictingChars(mTableModel.getColumnTitle(column));
                columnMap.put(ensureUniqueness(varName, varNameList), new Integer(column));
                }
            else if (mTableModel.isDescriptorColumn(column)) {
                String specialType = mTableModel.getColumnSpecialType(column);
                String parentName = mTableModel.getColumnTitle(mTableModel.getParentColumn(column));
                String varName = specialType+"_of_"+removeConflictingChars(parentName);
                columnMap.put(ensureUniqueness(varName, varNameList), new Integer(column));
                }
            }

		for (int i=0; i<mTableModel.getHitlistHandler().getHitlistCount(); i++) {
		    String varName = IS_MEMBER_OF + removeConflictingChars(mTableModel.getHitlistHandler().getHitlistName(i));
		    columnMap.put(ensureUniqueness(varName, varNameList), new Integer(CompoundTableHitlistHandler.getColumnFromHitlist(i)));
			}


        return columnMap;
		}

	/**
	 * Does any necessary preprocessing of the formula before the formula is passed to JEP.
	 * This includes running pseudo functions, which ask the user for input at runtime.
	 * @param formula
	 * @return formula with pseudo functions replaced by variables or null, if the user cancelled a dialog
	 */
	private String preprocessFormula(String formula) {
		formula = duplicateIDCodeBackslashes(formula);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_COLUMN_TITLE);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_COLUMN_VAR);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_STRING);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_NUMBER);
		return formula;
		}

	/**
	 * JEP evidently assumes that '\' within the formula escape the following character.
	 * Intentional '\' characters need to be passed as '\\'. Since idcodes may contain
	 * backslash characters, these need to be duplicated to correctly be treated by JEP. 
	 * @param formula
	 * @return
	 */
	private String duplicateIDCodeBackslashes(String formula) {
		if (formula == null)
			return null;

		int index = -1;
		while (true) {
			index = formula.indexOf(JEPChemSimilarityFunction.FUNCTION_NAME+"(", index+1);
			if (index == -1)
				break;

			index += JEPChemSimilarityFunction.FUNCTION_NAME.length() + 1;

			int index2 = formula.indexOf(')', index);
			if (index2 == -1)
				break;

			String substring = formula.substring(index, index2);
			if (substring.indexOf('\\') != -1)
				formula = replacePseudoFunction(formula, index, index2, substring.replace("\\", "\\\\"));

			index = index2;
			}

		return formula;
		}

	/**
	 * Converts pseudo functions of one type into variables.
	 * @param formula
	 * @return formula with pseudo functions replaced by variables or null, if the user cancelled a dialog
	 */
	private String resolvePseudoFunctions(String formula, final String functionName) {
		if (formula == null)
			return null;

		int index = -1;
		while (true) {
			index = formula.indexOf(functionName, index+1);
			if (index == -1)
				break;

			int index2 = index+functionName.length();
			String msg = extractPseudoFunctionMessage(formula, index2);
			if (msg == null || formula.charAt(index2+msg.length()+2) != ')')	// we expect a closing bracket
				return formula;	// we have a syntax error; let the parser deal with it

			if (SwingUtilities.isEventDispatchThread())
				mResolvedValue = ask(msg, functionName);
			else {
				try {
					final String _msg = msg;
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							mResolvedValue = ask(_msg, functionName);
							}
						});
					}
				catch (Exception e) {}
				}

			if (mResolvedValue == null)
				return null;

			formula = replacePseudoFunction(formula, index, index2+msg.length()+3, mResolvedValue);
			}

		return formula;
		}

	/**
	 * Extracts and returns the text between two double quotes.
	 * Returns null, if index doesn't point to a double quote or if closing double quote is not found
	 * of if the formula ends with the closing double quote.
	 * @param formula
	 * @param index of starting double quote
	 * @return null if syntax error or extracted message, which may be an empty String
	 */
	private String extractPseudoFunctionMessage(String formula, int index) {
		if (formula.charAt(index) != '"')
			return null;

		int index2 = formula.indexOf('"', index+1);
		if (index2 == -1 || index2+1 == formula.length())
			return null;

		return formula.substring(index+1, index2);
		}

	/**
	 * @param formula
	 * @param index1 first char of function name
	 * @param index2 first char after closing bracket
	 * @param varName
	 * @return formula with function replaced by variable name
	 */
	private String replacePseudoFunction(String formula, int index1, int index2, String varName) {
		return formula.substring(0, index1).concat(varName).concat(formula.substring(index2));
		}

	private String ask(final String msg, String functionName) {
		if (functionName.equals(PSEUDO_FUNCTION_ASK_STRING)) {
			String text = JOptionPane.showInputDialog(getParentFrame(), msg, "Define String Value", JOptionPane.QUESTION_MESSAGE);
			return "\""+text+"\"";
			}

		if (functionName.equals(PSEUDO_FUNCTION_ASK_NUMBER)) {
			while (true) {
				String text = JOptionPane.showInputDialog(getParentFrame(), msg, "Define Numerical Value", JOptionPane.QUESTION_MESSAGE);
				try {
					Double.parseDouble(text);
					}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(getParentFrame(), "'"+text+"' is not numeric. Try again.", "Number Format Error", JOptionPane.ERROR_MESSAGE);
					continue;
					}
				return text;
				}
			}

		if (functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_TITLE)
		 || functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_VAR)) {
			String[] columnNameList = new String[mTableModel.getTotalColumnCount()];
			for (int i=0; i<columnNameList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(i);
	
			String title = (String)JOptionPane.showInputDialog(getParentFrame(),
								msg,
								"Select Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			if (functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_TITLE))
				return "\""+title+"\"";

			int column = mTableModel.findColumn(title);
			for (String key:mRunTimeColumnMap.keySet())
				if (mRunTimeColumnMap.get(key).intValue() == column)
					return key;
			}

		return null;	// shouldn't happen
		}

	private boolean parseFormula(String formula) {
	    mParser = new JEP();
	    mParser.addStandardFunctions();
	    mParser.addStandardConstants();
	    mParser.addFunction("round", new JEPRoundFunction());
	    mParser.addFunction("int", new JEPIntFunction());
        mParser.addFunction("len", new JEPLenFunction());
        mParser.addFunction("contains", new JEPContainsFunction());
	    mParser.addFunction("ligeff1", new JEPRateHTSFunction(mTableModel));
        mParser.addFunction("ligeff2", new JEPLigEffFunction(mTableModel));
        mParser.addFunction(JEPChemSimilarityFunction.FUNCTION_NAME, new JEPChemSimilarityFunction(mTableModel));
        mParser.addFunction(JEPFrequencyFunction.FUNCTION_NAME, new JEPFrequencyFunction(mTableModel));
        mParser.addFunction(JEPRefValueOfCategoryFunction.FUNCTION_NAME, new JEPRefValueOfCategoryFunction(this, mTableModel));
        Iterator<String> keyIterator = mRunTimeColumnMap.keySet().iterator();
        while (keyIterator.hasNext()) {
            String varName = keyIterator.next();
            int column = mRunTimeColumnMap.get(varName).intValue();
            if (CompoundTableHitlistHandler.isHitlistColumn(column)) {
		        mParser.addVariable(varName, 0.0);
            	}
            else {
	            if (mTableModel.getColumnSpecialType(column) != null)
	                mParser.addVariable(varName, new JEPParameter(null, column));
	            else if (mTableModel.isColumnTypeDouble(column)
		         && !mTableModel.isColumnTypeDate(column))
			        mParser.addVariable(varName, 0.0);
			    else
			        mParser.addVariable(varName, "");
            	}
            }
	    mParser.addVariable(IS_VISIBLE_ROW, 0.0);
	    mParser.addVariable(IS_SELECTED_ROW, 0.0);

	    mParser.parseExpression(formula);
	    return mParser.hasError();
		}

	@Override
	public void runTask(Properties configuration) {
	    startProgress("Calculating values...", 0, mTableModel.getTotalRowCount());

	    String[] columnName = new String[1];
	    columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME);
	    int firstNewColumn = mTableModel.addNewColumns(columnName);
		mTableModel.setColumnDescription(configuration.getProperty(PROPERTY_FORMULA), firstNewColumn);

	    CompoundTableHitlistHandler hitlistHandler = mTableModel.getHitlistHandler();

	    for (mCurrentRow=0; mCurrentRow<mTableModel.getTotalRowCount(); mCurrentRow++) {
			if (threadMustDie())
			    break;
		    if (mCurrentRow % 16 == 0)
		        updateProgress(mCurrentRow);

		    CompoundRecord record = mTableModel.getTotalRecord(mCurrentRow);
            for (String varName:mRunTimeColumnMap.keySet()) {
                int column = mRunTimeColumnMap.get(varName).intValue();
                if (CompoundTableHitlistHandler.isHitlistColumn(column)) {
                	long hitlistMask = hitlistHandler.getHitlistMask(CompoundTableHitlistHandler.getHitlistFromColumn(column));
    		        mParser.addVariable(varName, (record.getFlags() & hitlistMask) != 0 ? 1.0 : 0.0 );
                	}
                else {
				    if (mTableModel.getColumnSpecialType(column) != null)
	                    mParser.addVariable(varName, new JEPParameter(record, column));
	                else if (mTableModel.isColumnTypeDouble(column)
			         && !mTableModel.isColumnTypeDate(column)) {
				        double value = mTableModel.getTotalOriginalDoubleAt(mCurrentRow, column);
	                    mParser.addVariable(varName, value);
	                    }
				    else
				        mParser.addVariable(varName, mTableModel.getValue(record, column));
                	}
                }
		    mParser.addVariable(IS_VISIBLE_ROW, mTableModel.isVisible(record) ? 1.0 : 0.0);
		    mParser.addVariable(IS_SELECTED_ROW, mTableModel.isVisibleAndSelected(record) ? 1.0 : 0.0);

		    Object o = mParser.getValueAsObject();
		    String value = (o == null) ? "NaN" : o.toString();
		    mTableModel.setTotalValueAt(value, mCurrentRow, firstNewColumn);
			}

		mTableModel.finalizeNewColumns(firstNewColumn, this);
		}

	/**
	 * During task execution this gets the currently executed row index.
	 * @return
	 */
	public int getCurrentRow() {
		return mCurrentRow;
		}

	private String removeConflictingChars(String s) {
	    StringBuffer buf = new StringBuffer(1+s.length());

	    // don't start with a digit to avoid implicit multiplication errors
	    if (Character.isDigit(s.charAt(0)))
	        buf.append('_');

	    for (int i=0; i<s.length(); i++) {
	        char theChar = s.charAt(i);
	        if (theChar < 128 && (Character.isLetterOrDigit(theChar) || theChar=='_'))
	            buf.append(theChar);
	    	}
	    return buf.toString();
		}

	private String ensureUniqueness(String varName, TreeSet<String> varNameList) {
	    if (!varNameList.contains(varName))
	        return varName;
	    int suffix = 2;
	    while (varNameList.contains(varName+"_"+suffix))
	        suffix++;
	    return varName+"_"+suffix;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalColumnCount() == 0 && mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("Neither data columns nor rows found.");
			return false;
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.put(PROPERTY_FORMULA, mTextFieldFormula.getText());
		configuration.put(PROPERTY_COLUMN_NAME, mTextFieldColumnName.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldFormula.setText(configuration.getProperty(PROPERTY_FORMULA, ""));
		mTextFieldColumnName.setText(configuration.getProperty(PROPERTY_COLUMN_NAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldFormula.setText("");
		mTextFieldColumnName.setText("Calculated Column");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
	    if (configuration.getProperty(PROPERTY_COLUMN_NAME, "").length() == 0) {
			showErrorMessage("No new column name specified.");
			return false;
	    	}
	    String formula = configuration.getProperty(PROPERTY_FORMULA, "");
	    if (formula.length() == 0) {
			showErrorMessage("No formula specified.");
			return false;
	    	}
		if (isLive) {
			mRunTimeColumnMap = createColumnMap();
			formula = preprocessFormula(formula);
			if (formula == null)
				return false;	// no error message because the user cancelled

			if (parseFormula(formula)) {
				showErrorMessage(mParser.getErrorInfo());
				return false;
				}
			}
		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}
	}
