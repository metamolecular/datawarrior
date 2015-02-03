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

package com.actelion.research.gui.form;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.actelion.research.gui.JVerticalBrowseToolbar;
import com.actelion.research.gui.PopupItemProvider;

public abstract class JResultDetailView extends JPanel
					implements ActionListener,ComponentListener,PopupItemProvider,ReferencedDataConsumer {
    private static final long serialVersionUID = 0x20110912;

    protected Component				mDetailView;
	private ReferenceResolver		mReferenceResolver;
	private ResultDetailPopupItemProvider mPopupItemProvider;
	private String					mDetailSource,mCurrentReference;
	private String[]				mDetailReference;
	private int						mCurrentDetailIndex;
	private boolean					mComponentVisible,mDetailNeedsUpdate,mToolbarVisible;
	private JVerticalBrowseToolbar	mToolbar;
    private ArrayList<JMenuItem>    mPopupItemList;

	public JResultDetailView(ReferenceResolver referenceResolver, ResultDetailPopupItemProvider popupItemProvider, String detailSource, Component detailView) {
		mReferenceResolver = referenceResolver;
		mPopupItemProvider = popupItemProvider;
		mDetailSource = detailSource;
		mDetailView = detailView;
		detailView.addComponentListener(this);
		setLayout(new BorderLayout());
		add(detailView, BorderLayout.CENTER);
		}

	public ReferenceResolver getReferenceResolver() {
		return mReferenceResolver;
		}

	public String getDetailSource() {
		return mDetailSource;
		}

	public void setReferenceResolver(ReferenceResolver resolver) {
		mReferenceResolver = resolver;
		}

	public void setDetailSource(String source) {
		mDetailSource = source;
		}

	public void setReferences(String[] detailReference) {
		if (mDetailReference == detailReference)
			return;

		boolean needsToolbar = (detailReference != null && detailReference.length > 1);
		if (needsToolbar && !mToolbarVisible) {
			if (mToolbar == null) {
				mToolbar = new JVerticalBrowseToolbar();
				mToolbar.addActionListener(this);
				}
			add(mToolbar, BorderLayout.EAST);
			validate();
			mToolbarVisible = true;
			}
		else if (!needsToolbar && mToolbarVisible) {
			remove(mToolbar);
			validate();
			mToolbarVisible = false;
			}

		setReference(detailReference == null ? null : detailReference[0]);

		mDetailReference = detailReference;
		mCurrentDetailIndex = 0;
		}

	/**
	 * Defines the popup item provider for additional popup items related to the result detail.
	 */
	public void setResultDetailPopupItemProvider(ResultDetailPopupItemProvider popupItemProvider) {
		mPopupItemProvider = popupItemProvider;
		}

	public void componentMoved(ComponentEvent e) {}

	public void componentResized(ComponentEvent e) {
		Dimension size = mDetailView.getSize();
		mComponentVisible = (size.width > 0 && size.height > 0);
		updateDetail();
		}

	public void componentHidden(ComponentEvent e) {
		mComponentVisible = false;
		}

	public void componentShown(ComponentEvent e) {
		mComponentVisible = true;
		updateDetail();
		}

	public abstract boolean hasOwnPopupMenu();

	/**
	 * We need two distinguish TWO cases:
	 * 1) The rendering component has its own popup menu and items:
	 * When creating its own popup menu, it should ask the PopupItemProvider
	 * (descendant of this class) with getPopupItems() for additional items to attach,
	 * i.e. the list of items added with addPopupItem().
	 * 2) The rendering component doesn't have an own popup menu. Then this class
	 * adds the needed MouseListener to the component and creates the popup menu
	 * containing all items added with addPopupItem().
	 * @return null or list of JMenuItems to be attached to popup menu
	 */
	@Override
	public JMenuItem[] getPopupItems() {
		if (mPopupItemList == null && mPopupItemProvider == null)
			return null;

		if (mPopupItemProvider == null)
			return mPopupItemList.toArray(new JMenuItem[0]);

		ArrayList<JMenuItem> itemList = mPopupItemProvider.getExternalPopupItems(mDetailSource, mCurrentReference);
		if (itemList == null)
			return (mPopupItemList == null) ? null : mPopupItemList.toArray(new JMenuItem[0]); 

		if (mPopupItemList != null)
			itemList.addAll(mPopupItemList);

		return itemList.toArray(new JMenuItem[0]);
		}

    /**
     * Creates a popup item using itemName as actionCommand and this detail view as
     * ActionListener. Then adds the popup item to the rendering component's popup menu.
     * If the rendering component does not have an own popup menu,
     * then the mechanism is created to provide one.
     * @param item
     */
    public void addPopupItem(String itemName) {
        JMenuItem item = new JMenuItem(itemName);
        item.addActionListener(this);
        addPopupItem(item);
        }

    /**
     * Adds a popup item to the rendering component's popup menu.
     * If the rendering component does not have an own popup menu,
     * then the mechanism is created to provide one.
     * @param item
     */
    public void addPopupItem(JMenuItem item) {
        if (mPopupItemList == null) {
            mPopupItemList = new ArrayList<JMenuItem>();
            if (!hasOwnPopupMenu()) {
	            getPopupComponent().addMouseListener(new MouseAdapter() {
	                public void mousePressed(MouseEvent e) {
	                    handlePopupTrigger(e);
	                    }
	                public void mouseReleased(MouseEvent e) {
	                    handlePopupTrigger(e);
	                    }
	                });
            	}
            }
        mPopupItemList.add(item);
        }

    private void handlePopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JPopupMenu popup = new JPopupMenu();
            for (JMenuItem item:mPopupItemList)
                popup.add(item);

            popup.show(getPopupComponent(), e.getX(), e.getY());
            }
        }

    private Component getPopupComponent() {
        return (mDetailView instanceof JScrollPane) ?
                ((JScrollPane)mDetailView).getViewport().getView() : mDetailView;
        }

    public void actionPerformed(ActionEvent e) {
		if (mDetailReference == null)	// shouldn't happen but the devil is a squirrel...
			return;

		if (e.getActionCommand().equals("|<")) {
			if (mCurrentDetailIndex > 0) {
				mCurrentDetailIndex = 0;
				setReference(mDetailReference[mCurrentDetailIndex]);
				}
			}
		else if (e.getActionCommand().equals("<")) {
			if (mCurrentDetailIndex > 0) {
				mCurrentDetailIndex--;
				setReference(mDetailReference[mCurrentDetailIndex]);
				}
			}
		else if (e.getActionCommand().equals(">")) {
			if (mCurrentDetailIndex < mDetailReference.length-1) {
				mCurrentDetailIndex++;
				setReference(mDetailReference[mCurrentDetailIndex]);
				}
			}
		else if (e.getActionCommand().equals(">|")) {
			if (mCurrentDetailIndex < mDetailReference.length-1) {
				mCurrentDetailIndex = mDetailReference.length-1;
				setReference(mDetailReference[mCurrentDetailIndex]);
				}
			}
		}

	public String getCurrentReference() {
		return mCurrentReference;
		}

	private void setReference(String currentReference) {
		if (mCurrentReference == null && currentReference == null)
			return;
		if (mCurrentReference != null && currentReference != null && mCurrentReference.equals(currentReference))
			return;

		mCurrentReference = currentReference;
		mDetailNeedsUpdate = true;
		updateDetail();
		}

	private void updateDetail() {
		if (mComponentVisible && mDetailNeedsUpdate) {
			if (mCurrentReference == null)
				setDetailData(null);
			else
				mReferenceResolver.requestData(mDetailSource, mCurrentReference, ReferenceResolver.MODE_DEFAULT, this);

			mDetailNeedsUpdate = false;
			}
		}

	public void setReferencedData(String source, String reference, byte[] data) {
		if (reference.equals(mCurrentReference))
			setDetailData(data);
		}

	protected void printReferences(Graphics g, Rectangle2D.Float r, float scale, String[] detailReference) {
		if (detailReference == null)	// shouldn't happen but the devil is a squirrel...
			return;

			// Currently prints only first referenced object.
			// Could alternatively print all resolved references within r.
		print(g, r, scale, mReferenceResolver.resolveReference(mDetailSource, detailReference[0], ReferenceResolver.MODE_FULL_IMAGE));
		}

	abstract protected void print(Graphics g, Rectangle2D.Float r, float scale, Object data);
	abstract protected void setDetailData(Object data);
	}
