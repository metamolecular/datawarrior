package com.actelion.research.gui.dock;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.actelion.research.gui.HeaderPaintHelper;

public class DockableHeader extends JPanel implements ActionListener {
    private static final long serialVersionUID = 0x20070723;

    private Dockable mDockable;
    private JLabel mTitleLabel;
    private ActionListener mActionListener;
    private HeaderMouseAdapter mMouseAdapter;
    private boolean mIsSelected;

    public DockableHeader(Dockable dockable, String title, ActionListener al, boolean isClosable, boolean hasMenuButton) {
        super(new BorderLayout());

        mDockable = dockable;
        mTitleLabel = new JLabel(title, SwingConstants.LEADING) {
            private static final long serialVersionUID = 0x20080423;
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 16);
                }
            };
        mTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        mTitleLabel.setOpaque(false);
        add(mTitleLabel, BorderLayout.WEST);

        JToolBar bar = createToolBar(isClosable, hasMenuButton);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        bar.setOpaque(false);
        add(bar, BorderLayout.EAST);

        setOpaque(true);
        mActionListener = al;
        mMouseAdapter = new HeaderMouseAdapter(this);
        addMouseListener(mMouseAdapter);
        addMouseMotionListener(mMouseAdapter);
        }

    public String getTitle() {
        return mTitleLabel.getText();
        }
        
    public void setTitle(String title) {
        mTitleLabel.setText(title);
        }

    public Dockable getDockable() {
        return mDockable;
        }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        Paint storedPaint = g2.getPaint();

        g2.setPaint(HeaderPaintHelper.getHeaderPaint(mIsSelected, height));
        g2.fillRect(0, 0, width, height);

        g2.setPaint(storedPaint);
        }

    public void update(boolean isSelected) {
        mIsSelected = isSelected;
        repaint();
        }

    public PopupProvider getPopupProvider() {
        return mMouseAdapter.getPopupProvider();
        }

    public void setPopupProvider(PopupProvider p) {
        mMouseAdapter.setPopupProvider(p);
        }

    private JToolBar createToolBar(boolean isClosable, boolean hasPopupButton) {
        JToolBar toolbar = new JToolBar();

        if (hasPopupButton) {
            JButton popupButton = createButton(createIcon("axisButton.png"));
            popupButton.addActionListener(this);
            popupButton.setActionCommand("popup");
            toolbar.add(popupButton);
            }

        JButton maxButton = createButton(createIcon("maxButton.png"));
        maxButton.addActionListener(this);
        maxButton.setActionCommand("max");
        toolbar.add(maxButton);

        if (isClosable) {
            JButton closeButton = createButton(createIcon("closeButton.png"));
            closeButton.addActionListener(this);
            closeButton.setActionCommand("close");
            toolbar.add(closeButton);
            }

        toolbar.setFloatable(false);
        toolbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        return toolbar;
        }

    private JButton createButton(Icon icon) {
        JButton button = new JButton(icon);
        button.setFocusable(false);
        return button;
        }

    public void actionPerformed(ActionEvent e) {
        String title = mTitleLabel.getText();
        if (e.getActionCommand().equals("popup")) {
            mActionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "popup_"+title));
            return;
            }

        if (e.getActionCommand().equals("max")) {
            mActionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "max_"+title));
            return;
            }

        if (e.getActionCommand().equals("close")) {
            mActionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close_"+title));
            return;
            }
        }

    private Icon createIcon(String icon) {
        return new ImageIcon(createImageImpl(icon));
        }

    private Image createImageImpl(String imageFileName) {
        URL iconURL = getClass().getResource("/images/"+imageFileName);
        if (iconURL == null)
            throw new RuntimeException("Could not find: " + imageFileName);

        return Toolkit.getDefaultToolkit().createImage(iconURL);
        }

    /**
     * Determines and answers the header's background color. Tries to lookup a special color from the L&F.
     * In case it is absent, it uses the standard internal frame background.
     * 
     * @return the color of the header's background
     *
    private Color getHeaderBackground() {
        Color c = UIManager.getColor("SimpleInternalFrame.activeTitleBackground");
        if (c != null)
            return c;

        return UIManager.getColor("InternalFrame.activeTitleBackground");
        }*/

    /**
     * Determines and answers the header's text foreground color. Tries to lookup a special color from the
     * L&amp;F. In case it is absent, it uses the standard internal frame forground.
     * 
     * @param selected true to lookup the active color, false for the inactive
     * @return the color of the foreground text
     *
    private Color getTextForeground(boolean selected) {
        Color c = UIManager.getColor(selected ? "SimpleInternalFrame.activeTitleForeground"
            : "SimpleInternalFrame.inactiveTitleForeground");
        if (c != null)
            return c;

        return UIManager.getColor(selected ? "InternalFrame.activeTitleForeground" : "Label.foreground");
        }*/
    }
