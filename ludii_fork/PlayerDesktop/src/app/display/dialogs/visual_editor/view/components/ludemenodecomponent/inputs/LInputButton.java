package app.display.dialogs.visual_editor.view.components.ludemenodecomponent.inputs;


import app.display.dialogs.visual_editor.view.designPalettes.DesignPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class LInputButton extends JButton {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6627034885726770235L;
	public Color ACTIVE_COLOR;
    private final Color HOVER_COLOR = new Color(127,191,255);

    public ImageIcon ACTIVE_ICON;
    private ImageIcon HOVER_ICON;
    private boolean active = true;

    public LInputButton(ImageIcon activeIcon, ImageIcon hoverIcon)
    {
        super(activeIcon);
        this.ACTIVE_COLOR = DesignPalette.FONT_LUDEME_INPUTS_COLOR();
        this.ACTIVE_ICON = activeIcon;
        this.HOVER_ICON = hoverIcon;

        setFont(new Font("Roboto Bold", 0, 12));

        // make transparent
        setBorder(BorderFactory.createEmptyBorder());
        setFocusPainted(false);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);

        addMouseListener(hoverMouseListener);
    }

    public void updateDP()
    {
        if(active)
            setActive();
    }

    public void setActive(){
        active = true;
        setForeground(ACTIVE_COLOR);
        setIcon(ACTIVE_ICON);
        repaint();
    }


    public void setHover(){
        active = false;
        setForeground(HOVER_COLOR);
        setIcon(HOVER_ICON);
        repaint();
    }

    final MouseListener hoverMouseListener = new MouseAdapter() {
        @Override
		public void mouseEntered(MouseEvent e) {
            setHover();
        }
        @Override
		public void mouseExited(MouseEvent e) {
            setActive();
        }
    };

    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        ACTIVE_ICON = new ImageIcon(ACTIVE_ICON.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        HOVER_ICON = new ImageIcon(HOVER_ICON.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        if(active) setIcon(ACTIVE_ICON);
        else setIcon(HOVER_ICON);
    }
}
