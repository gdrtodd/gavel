package app.display.dialogs.visual_editor.view.panels.editor;

import app.display.dialogs.visual_editor.handler.Handler;
import app.display.dialogs.visual_editor.view.designPalettes.DesignPalette;
import app.display.dialogs.visual_editor.view.panels.IGraphPanel;
import app.display.dialogs.visual_editor.view.panels.editor.gameEditor.GameGraphPanel;

import javax.swing.*;
import java.awt.*;

public class EditorPopupMenu extends JPopupMenu
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 5378508370225281683L;

	public EditorPopupMenu(IGraphPanel graphPanel, int x, int y)
    {
        JMenuItem newLudeme = new JMenuItem("New Ludeme");
        JMenuItem paste = new JMenuItem("Paste");

        paste.addActionListener(e -> {
            Handler.paste(graphPanel.graph(), x, y);
            // deselect all previously selected nodes
            graphPanel.deselectEverything();
        });

        JMenuItem compact = new JMenuItem("Arrange graph");
        compact.addActionListener(e -> graphPanel.getLayoutHandler().executeLayout());

        newLudeme.addActionListener(e -> graphPanel.showAllAvailableLudemes());

        JMenuItem collapse = new JMenuItem("Collapse");
        collapse.addActionListener(e -> Handler.collapse(graphPanel.graph()));

        int iconHeight = (int)(newLudeme.getPreferredSize().getHeight()*0.75);

        ImageIcon newLudemeIcon = new ImageIcon(DesignPalette.ADD_ICON.getImage().getScaledInstance(iconHeight, iconHeight, Image.SCALE_SMOOTH));
        newLudeme.setIcon(newLudemeIcon);
        ImageIcon pasteIcon = new ImageIcon(DesignPalette.PASTE_ICON.getImage().getScaledInstance(iconHeight, iconHeight, Image.SCALE_SMOOTH));
        paste.setIcon(pasteIcon);
        ImageIcon collapseIcon = new ImageIcon(DesignPalette.COLLAPSE_ICON().getImage().getScaledInstance(iconHeight, iconHeight, Image.SCALE_SMOOTH));
        collapse.setIcon(collapseIcon);

        paste.setEnabled(!Handler.clipboard().isEmpty());

        collapse.setEnabled(!Handler.selectedNodes(graphPanel.graph()).isEmpty());

        add(newLudeme);

        if(graphPanel instanceof GameGraphPanel)
        {
            JMenuItem addDefine = new JMenuItem("Add Define");
            GameGraphPanel ggp = (GameGraphPanel) graphPanel;
            addDefine.addActionListener(e -> ggp.showAddDefinePanel());
            add(addDefine);
        }

        add(paste);
        add(compact);
        add(collapse);

        JMenuItem fix = new JMenuItem("Fix group");
        fix.addActionListener(e -> graphPanel.graph().getNode(graphPanel.graph().selectedRoot()).setFixed(true));

        JMenuItem unfix = new JMenuItem("Unfix group");
        unfix.addActionListener(e -> graphPanel.graph().getNode(graphPanel.graph().selectedRoot()).setFixed(false));

        if (graphPanel.graph().selectedRoot() != -1 &&
                graphPanel.graph().getNode(graphPanel.graph().selectedRoot()).fixed())
        {
            add(unfix);
            graphPanel.repaint();
        }
        else if (graphPanel.graph().selectedRoot() != -1)
        {
            add(fix);
            graphPanel.repaint();
        }


        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(e -> Handler.undo());
        add(undo);

        JMenuItem redo = new JMenuItem("Redo");
        redo.addActionListener(e -> Handler.redo());
        add(redo);
    }

}
