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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JImagePanel;
import com.actelion.research.gui.JMultiPanelView;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.form.JHTMLDetailView;
import com.actelion.research.gui.form.JImageDetailView;
import com.actelion.research.gui.form.JResultDetailView;
import com.actelion.research.gui.form.JSVGDetailView;
import com.actelion.research.gui.jmol.MoleculeViewer;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.DetailTableModel;
import com.actelion.research.table.HighlightListener;
import com.actelion.research.table.JDetailTable;
import com.actelion.research.table.view.VisualizationColor;

public class DEDetailPane extends JMultiPanelView implements HighlightListener,CompoundTableListener,CompoundTableColorHandler.ColorListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String STRUCTURE = "Structure";
    private static final String STRUCTURE_3D = "3D-Structure";
    private static final String RECORD_DATA = "Data";
    private static final String IMAGE = "Image";
    protected static final String RESULT_DETAIL = "Detail";

	private CompoundTableModel	mTableModel;
	private CompoundRecord		mCurrentRecord;
	private DetailTableModel	mDetailModel;
	private JDetailTable		mDetailTable;
	private ArrayList<DetailViewInfo> mDetailViewList;

	public DEDetailPane(CompoundTableModel tableModel) {
		super();
		mTableModel = tableModel;
	    mTableModel.addCompoundTableListener(this);
        mTableModel.addHighlightListener(this);

		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(100, 100));

		mDetailModel = new DetailTableModel(mTableModel);
  		mDetailTable = new JDetailTable(mDetailModel);
        mDetailTable.putClientProperty("Quaqua.Table.style", "striped");

        // to eliminate the disabled default action of the JTable when typing menu-V
        mDetailTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mDetailTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, RECORD_DATA);

		mDetailViewList = new ArrayList<DetailViewInfo>();
		}

    public void setColorHandler(CompoundTableColorHandler colorHandler) {
    	mDetailTable.setColorHandler(colorHandler);
    	colorHandler.addColorListener(this);
    	}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
		    int firstNewView = mDetailViewList.size();
            addColumnDetailViews(e.getSpecifier());
            
            for (int i=firstNewView; i<mDetailViewList.size(); i++)
                updateDetailView(mDetailViewList.get(i));
			}
		else if (e.getType() == CompoundTableEvent.cNewTable) {
            mCurrentRecord = null;

            for (DetailViewInfo viewInfo:mDetailViewList)
				remove(viewInfo.view);
			mDetailViewList.clear();

            addColumnDetailViews(0);
			}
        else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
            for (DetailViewInfo viewInfo:mDetailViewList)
                if (e.getSpecifier() == viewInfo.column)
                    updateDetailView(viewInfo);
            }
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			for (int i=mDetailViewList.size()-1; i>=0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				viewInfo.column = e.getMapping()[viewInfo.column];

                    // for STRUCTURE(_3D) types viewInfo.detail contains the coordinate column
                if (viewInfo.type.equals(STRUCTURE)) {
                    if (viewInfo.detail != -1)  // 2D-coords may be generated on-the-fly
                        viewInfo.detail = e.getMapping()[viewInfo.detail];
                    }
                if (viewInfo.type.equals(STRUCTURE_3D)) {
                    viewInfo.detail = e.getMapping()[viewInfo.detail];
                    if (viewInfo.detail == -1)  // remove view if 3D-coords missing
                        viewInfo.column = -1;
                    }

                if (viewInfo.column == -1) {
					remove(viewInfo.view);
					mDetailViewList.remove(i);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumnDetails) {
			for (int i=mDetailViewList.size()-1; i>=0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				if (viewInfo.type.equals(RESULT_DETAIL) && e.getSpecifier() == viewInfo.column) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1) {
						remove(viewInfo.view);
						mDetailViewList.remove(i);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnDetailSource) {
            for (DetailViewInfo viewInfo:mDetailViewList) {
                if (viewInfo.type.equals(RESULT_DETAIL)) {
    				int column = e.getSpecifier();
    				int detail = e.getMapping()[0];
    				if (column == viewInfo.column
    				 && detail == viewInfo.detail) {
    				    String source = mTableModel.getColumnDetailSource(column, detail);
    				    ((JResultDetailView)viewInfo.view).setDetailSource(source);
    					}
    				}
			    }
			}
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
        for (DetailViewInfo viewInfo:mDetailViewList) {
            if (viewInfo.column == column) {
            	if (viewInfo.type.equals(STRUCTURE)) {
                    updateDetailView(viewInfo);
            		}
            	}
            }
        mDetailTable.repaint();
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	protected void addColumnDetailViews(int firstColumn) {
        for (int column=firstColumn; column<mTableModel.getTotalColumnCount(); column++) {
            String columnName = mTableModel.getColumnTitleNoAlias(column);
            String specialType = mTableModel.getColumnSpecialType(column);
            if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
                int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
                JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
                view.setBackground(Color.white);
                view.setClipboardHandler(new ClipboardHandler());
                addColumnDetailView(view, column, coordinateColumn, STRUCTURE, mTableModel.getColumnTitle(column));
                continue;
                }
            if (CompoundTableModel.cColumnType3DCoordinates.equals(specialType)) {
                MoleculeViewer view = new MoleculeViewer();
                addColumnDetailView(view, mTableModel.getParentColumn(column), column, STRUCTURE_3D,
                					mTableModel.getColumnTitle(column));
                continue;
                }
            if (columnName.equalsIgnoreCase("imagefilename")
             || columnName.equals("#Image#")
             || mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath) != null) {
                boolean useThumbNail = (mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyUseThumbNail) != null);
                String imagePath = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath);
                if (imagePath == null)
                    imagePath = FileHelper.getCurrentDirectory() + File.separator + "images" + File.separator;
                JImagePanel view = new JImagePanel(imagePath, useThumbNail);
                String viewName = (columnName.equalsIgnoreCase("imagefilename")
                                || columnName.equals("#Image#")) ? IMAGE : mTableModel.getColumnTitle(column);
                addColumnDetailView(view, column, -1, IMAGE, viewName);
                continue;
                }
            for (int detail=0; detail<mTableModel.getColumnDetailCount(column); detail++) {
                JResultDetailView view = null;
                String detailType = mTableModel.getColumnDetailType(column, detail);
                if (detailType.equals(JHTMLDetailView.TYPE_TEXT_PLAIN)
                	  || detailType.equals(JHTMLDetailView.TYPE_TEXT_HTML))
                    view = new JHTMLDetailView(mTableModel.getDetailHandler(),
                    						   mTableModel.getDetailHandler(),
                                               mTableModel.getColumnDetailSource(column, detail),
                                               detailType);
                else if (detailType.equals(JImageDetailView.TYPE_IMAGE_JPEG)
                      || detailType.equals(JImageDetailView.TYPE_IMAGE_GIF)
                      || detailType.equals(JImageDetailView.TYPE_IMAGE_PNG)) {
                    view = new JImageDetailView(mTableModel.getDetailHandler(),
                    							mTableModel.getDetailHandler(),
                                                mTableModel.getColumnDetailSource(column, detail));
                    ((JImageDetailView)view).setUseThumbNail(useThumbNails(
                    							mTableModel.getColumnDetailSource(column, detail)));
                	}
                else if (detailType.equals(JSVGDetailView.TYPE_IMAGE_SVG))
                    view = new JSVGDetailView(mTableModel.getDetailHandler(),
                    						  mTableModel.getDetailHandler(),
                                              mTableModel.getColumnDetailSource(column, detail));

                if (view != null) {
                	addColumnDetailView(view, column, detail, RESULT_DETAIL, mTableModel.getColumnDetailName(column, detail));
                    }
                }
            }
        }

	protected void addColumnDetailView(JComponent view, int column, int detail, String type, String title) {
        mDetailViewList.add(new DetailViewInfo(view, column, detail, type));
        add(view, title);
		}

	// override this if detail sources support thumbnails
	protected boolean useThumbNails(String detailSource) {
		return false;
		}

	public void highlightChanged(CompoundRecord record) {
    	if (record != null) {
			mCurrentRecord = record;
	
	        for (DetailViewInfo viewInfo:mDetailViewList)
	            updateDetailView(viewInfo);
	
			mDetailModel.detailChanged(record);
    		}
		}

    private void updateDetailView(DetailViewInfo viewInfo) {
        if (viewInfo.type.equals(STRUCTURE)) {
            StereoMolecule mol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
            StereoMolecule displayMol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
            ((JStructureView)viewInfo.view).structureChanged(mol, displayMol);
            }
        else if (viewInfo.type.equals(STRUCTURE_3D)) {
            StereoMolecule mol = mTableModel.getChemicalStructure(mCurrentRecord, viewInfo.detail, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
            if (mol == null || mol.getAllBonds() == 0)
                ((MoleculeViewer)viewInfo.view).setMolecule(new FFMolecule());
            else {
                ((MoleculeViewer)viewInfo.view).setMolecule(new FFMolecule(mol));
                ((MoleculeViewer)viewInfo.view).center();
                ((MoleculeViewer)viewInfo.view).repaint();
                }
            }
        else if (viewInfo.type.equals(IMAGE)) {
            ((JImagePanel)viewInfo.view).setFileName((mCurrentRecord == null) ? null
                            : mTableModel.encodeData(mCurrentRecord, viewInfo.column));
            }
        else if (viewInfo.type.equals(RESULT_DETAIL)) {
            if (mCurrentRecord == null) {
                ((JResultDetailView)viewInfo.view).setReferences(null);
                }
            else {
                String[][] reference = mCurrentRecord.getDetailReferences(viewInfo.column);
                ((JResultDetailView)viewInfo.view).setReferences(reference == null
                                            || reference.length<=viewInfo.detail ?
                            null : reference[viewInfo.detail]);
                }
            }
        }
    }

class DetailViewInfo {
    public JComponent view;
    public int column,detail;
    public String type;

    public DetailViewInfo(JComponent view, int column, int detail, String type) {
        this.view = view;
        this.column = column;   // is idcode column in case of STRUCTURE(_3D)
        this.detail = detail;   // is coordinate column in case of STRUCTURE(_3D)
        this.type = type;
        }
    }
