package com.actelion.research.gui.dock;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * A lightweight component derived from JPanel that features certain subcomponents
 * that make it look similar to a frame or internal frame. It shows a titlebar above
 * its content component. The title bar has an
 * optional icon on the left, a title text and an optional toolbar on the right.
 */
public class Dockable extends JPanel {
    private static final long serialVersionUID = 0x20070720;

    private DockableHeader mHeader;
    private Component mContent;
    private boolean mIsSelected,mIsVisible,mHasMenuButton;

    /**
     * Constructs a <code>TitledComponent</code> with the specified title and content
     * panel.
     * 
     * @param content
     * @param title the initial title, which must be unique within the JDockingPanel
     * @param al the ActionListener to receive close_<title> and max_<title> action commands
     * @param isClosable determines whether the title area contains a close button
     */
    public Dockable(Component content, String title, ActionListener al, boolean isClosable) {
    	this(content, title, al, isClosable, false);
    	}

    /**
     * Constructs a <code>TitledComponent</code> with the specified title and content
     * panel.
     * 
     * @param content
     * @param title the initial title, which must be unique within the JDockingPanel
     * @param al the ActionListener to receive close_<i>title</i>, max_<i>title</i> and popup_<i>title</i> action events
     * @param isClosable determines whether the title area contains a close button
     * @param hasMenuButton determines whether the title area contains a pupup button
     */
    public Dockable(Component content, String title, ActionListener al, boolean isClosable, boolean hasMenuButton) {
        super(new BorderLayout());
        setBorder(new ShadowBorder());

        mContent = content;
        mHeader = new DockableHeader(this, title, al, isClosable, hasMenuButton);
        mIsVisible = false;
        mHasMenuButton = hasMenuButton;
        add(mHeader, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        setSelected(true);
        mHeader.update(mIsSelected);
        }

    /**
     * Sets the minimum size of the encapsulated content component
     * and sets the minimum width of the dockable header.
     * This effectively limits the minimum size of the content component.
     */
    public void setContentMinimumSize(Dimension size) {
    	mContent.setMinimumSize(size);
    	mHeader.setMinimumSize(new Dimension(size.width, mHeader.getPreferredSize().height));
    	}

    /**
     * Returns the title of dockable header.
     * @return String the current title text
     */
    public String getTitle() {
        return mHeader.getTitle();
    }

    /**
     * Sets a new title text of the dockable header.
     * @param newText the title text to be set
     */
    public void setTitle(String newText) {
        String oldText = getTitle();
        mHeader.setTitle(newText);
        firePropertyChange("title", oldText, newText);
    	}

    /**
     * Removes the content temporarily to be added to another container
     * Must be followed later by a endBorrowContent() call.
     * @return the current content
     */
    public Component borrowContent() {
        remove(mContent);
        return mContent;
        }

    /**
     * Returns the content from temporarily adding it to another copntainer.
     * Must be followed later by a returnContent() call.
     * @return the current content
     */
    public void endBorrowContent() {
        add(mContent, BorderLayout.CENTER);
        }

    /**
     * Returns the content - null, if none has been set.
     * 
     * @return the current content
     */
    public Component getContent() {
        return mContent;
        }

    /**
     * Answers if the panel is currently selected, i.e. the active one. In the selected state,
     * the header background is rendered differently.
     * @return boolean
     */
    public boolean isSelected() {
        return mIsSelected;
    	}

    /**
     * Method used by the JDockingPanel to inform the dockable when its visibility changes.
     * @param isVisible new visibility
     */
    protected void notifyVisibility(boolean isVisible) {
    	mIsVisible = isVisible;
    	}

    /**
     * Answers if the panel is currently visible, i.e. docked and not hidden by other dockables in a tabbed pane.
     * @return boolean
     */
    public boolean isVisibleDockable() {
        return mIsVisible;
    	}

    /**
     * This panel draws its title bar differently if it is selected, which may be used to indicate to the user
     * that this panel has the focus, or should get more attention than other simple internal frames.
     * 
     * @param newValue a boolean, where true means the frame is selected (currently active) and false means it
     *            is not
     */
    public void setSelected(boolean selected) {
    	if (mIsSelected != selected) {
	        mIsSelected = selected;
	        mHeader.update(selected);
	        firePropertyChange("selected", !selected, selected);
    		}
    	}

    /**
     * Updates the UI. In addition to the superclass behavior, we need to update the header component.
     */
    public void updateUI() {
        super.updateUI();
        if (mHeader != null)
            mHeader.update(mIsSelected);
        }

    public Component getDragHandle() {
        return mHeader;
        }

    public PopupProvider getPopupProvider() {
        return mHeader.getPopupProvider();
        }

    public void setPopupProvider(PopupProvider p) {
        mHeader.setPopupProvider(p);
        }

    public boolean hasMenuButton() {
    	return mHasMenuButton;
    	}
	}
