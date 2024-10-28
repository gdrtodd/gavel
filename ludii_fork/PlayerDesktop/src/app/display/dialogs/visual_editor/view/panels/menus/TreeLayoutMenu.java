package app.display.dialogs.visual_editor.view.panels.menus;

import app.display.dialogs.visual_editor.LayoutManagement.GraphAnimator;
import app.display.dialogs.visual_editor.handler.Handler;
import app.display.dialogs.visual_editor.view.panels.editor.EditorSidebar;
import app.display.dialogs.visual_editor.view.panels.editor.tabPanels.LayoutSettingsPanel;
import app.display.dialogs.visual_editor.view.panels.userGuide.LayoutUserGuideFrame;

import javax.swing.*;

public class TreeLayoutMenu extends JMenu
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -9127390803169376994L;
	public static final JMenuItem undoP = new JMenuItem("Undo Placement");
    public static final JMenuItem redoP = new JMenuItem("Redo Placement");

    /**
     * Constructor
     */
    public TreeLayoutMenu(EditorMenuBar menuBar)
    {
        super("Layout");

        undoP.setEnabled(false);
        redoP.setEnabled(false);

        undoP.addActionListener(e -> {
            GraphAnimator.getGraphAnimator().updateToInitPositions();
            undoP.setEnabled(false);
            redoP.setEnabled(true);
            Handler.gameGraphPanel.repaint();
        });

        redoP.addActionListener(e -> {
            GraphAnimator.getGraphAnimator().updateToFinalPositions();
            undoP.setEnabled(true);
            redoP.setEnabled(false);
            Handler.gameGraphPanel.repaint();
        });

        EditorMenuBar.addJMenuItem(this, "Arrange Graph",
                Handler.currentGraphPanel.getLayoutHandler().getEvaluateAndArrange());

        add(new JSeparator());

        add(undoP);
        add(redoP);

        add(new JSeparator());

        EditorMenuBar.addJCheckBoxMenuItem(this, "Animation", Handler.animation, e -> {
            LayoutSettingsPanel.getLayoutSettingsPanel().animatePlacement().setSelected(((JCheckBoxMenuItem) e.getSource()).isSelected());
            Handler.animation = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        });
        EditorMenuBar.addJCheckBoxMenuItem(this, "Auto Placement", Handler.autoplacement, e -> {
            LayoutSettingsPanel.getLayoutSettingsPanel().autoPlacement().setSelected(((JCheckBoxMenuItem) e.getSource()).isSelected());
            Handler.autoplacement = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        });
        EditorMenuBar.addJCheckBoxMenuItem(this, "Preserve Configurations", Handler.autoplacement, e -> Handler.evaluateLayoutMetrics = ((JCheckBoxMenuItem) e.getSource()).isSelected());

        add(new JSeparator());

        EditorMenuBar.addJMenuItem(this, "Open Layout Settings", e -> {
            EditorSidebar.getEditorSidebar().setVisible(true);
            EditorSidebar.getEditorSidebar().setLayoutTabSelected();
        });

        EditorMenuBar.addJMenuItem(this, "Help", e -> new LayoutUserGuideFrame());

    }


}
