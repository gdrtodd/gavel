package app.display.dialogs.visual_editor.LayoutManagement;

import app.display.dialogs.visual_editor.handler.Handler;
import app.display.dialogs.visual_editor.model.interfaces.iGNode;
import app.display.dialogs.visual_editor.model.interfaces.iGraph;
import app.display.dialogs.visual_editor.view.panels.editor.tabPanels.LayoutSettingsPanel;
import app.display.dialogs.visual_editor.view.panels.menus.TreeLayoutMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import static app.display.dialogs.visual_editor.LayoutManagement.GraphRoutines.updateNodeDepth;

/**
 * @author nic0gin
 */
public class LayoutHandler
{
    private static final boolean RECORD_TIME = false;

    private final iGraph graph;
    private final DFBoxDrawing layout;

    private final EvaluateAndArrange evaluateAndArrange = new EvaluateAndArrange();

    /**
     * Constructor
     * @param graph graph layout of which should be handled
     */
    public LayoutHandler(iGraph graph)
    {
        this.graph = graph;
        layout = new DFBoxDrawing(graph, RECORD_TIME);
    }

    /**
     * Update compactness value of layout
     * @param sliderValue value between 0.0 and 1.0
     */
    public void updateCompactness(double sliderValue)
    {
        layout.setCompactness(sliderValue);
    }

    /**
     * Explicitly update layout metrics
     */
	public void updateDFSWeights(double offset, double distance, double spread)
    {
        layout.updateWeights(Double.valueOf(offset), Double.valueOf(distance), Double.valueOf(spread));
    }

    /**
     * Implicitly update layout metrics i.e. evaluate user's placement
     */
    public void evaluateGraphWeights()
    {
        long startTime = System.nanoTime();
        double[] weights = GraphRoutines.computeLayoutMetrics(graph, graph.getRoot().id());
        long endTime = System.nanoTime();
        if (RECORD_TIME) System.out.println("Metric computation: "+ (endTime - startTime)/1E6);
        layout.updateWeights(weights);
        LayoutSettingsPanel.getLayoutSettingsPanel().updateSliderValues(weights[0], weights[1], weights[2]);
    }

    /**
     * Executes layout management procedures of a graph
     */
    public void executeLayout()
    {
        if (RECORD_TIME) System.out.println("Nodes: " + graph.getNodeList().size());
        GraphAnimator.getGraphAnimator().clearPositionHistory();
        if (Handler.evaluateLayoutMetrics) evaluateGraphWeights();

        long startTime = System.nanoTime();
        arrangeTreeComponents();
        long endTime = System.nanoTime();
        if (RECORD_TIME) System.out.println("Total drawing time: "+ (endTime - startTime)/1E6);

        TreeLayoutMenu.redoP.setEnabled(false);
        TreeLayoutMenu.undoP.setEnabled(true);
        Handler.currentGraphPanel.deselectEverything();
    }

    /**
     * Arranges layout of all components of a graph
     */
	public void arrangeTreeComponents()
    {
        if (GraphAnimator.getGraphAnimator().updateCounter() != 0) return;

        // determine graph components to be updated
        List<Integer> roots;
        if (graph.selectedRoot() == -1)
        {
            roots = new ArrayList<>(graph.connectedComponentRoots());
            // move root node at the top of the list
            roots.remove(Integer.valueOf(graph.getRoot().id()));
            roots.add(0, Integer.valueOf(graph.getRoot().id()));
        }
        else
        {
            roots = new ArrayList<>();
            roots.add(Integer.valueOf(graph.selectedRoot()));
        }



        // update graph components
        Vector2D transPos = null;
        for (int i = 0; i < roots.size(); i++)
        {
            int root = roots.get(i).intValue();

            // translation position
            if (i > 0)
            {
                Rectangle rect = GraphRoutines.getSubtreeArea(graph, roots.get(i-1).intValue());
                transPos = new Vector2D(
                        Handler.gameGraphPanel.parentScrollPane().getViewport().getViewRect().x+NodePlacementRoutines.DEFAULT_X_POS,
                        rect.y+rect.height+NodePlacementRoutines.DEFAULT_Y_POS);
            }

            // check if has children
            if (!graph.getNode(root).children().isEmpty())
            {
                // preserve initial positions of subtree
                if (Handler.animation) GraphAnimator.getGraphAnimator().preserveInitPositions(graph, root);
                // update depth of subtree
                updateNodeDepth(graph, root);
                // rearrange subtree layout with standard procedure
                layout.applyLayout(root);
                if (i > 0) NodePlacementRoutines.translateByRoot(graph, root, transPos);
                // preserve final position
                if (Handler.animation) GraphAnimator.getGraphAnimator().preserveFinalPositions();
            }
            else
            {
                iGNode rNode = graph.getNode(root);
                // preserve initial position of single node
                if (Handler.animation) GraphAnimator.getGraphAnimator().nodeInitPositions().put(rNode, rNode.pos());
                if (i > 0) NodePlacementRoutines.translateByRoot(graph, root, transPos);
                // preserve final position of single node
                if (Handler.animation) GraphAnimator.getGraphAnimator().nodeFinalPosition().put(rNode, rNode.pos());
            }
        }



        // display updated positions either through animation or single jump
        if (Handler.animation)
        {
            // animate change
            Timer animationTimer = new Timer(3, e -> {
                if (GraphAnimator.getGraphAnimator().animateGraphNodes())
                {
                    ((Timer)e.getSource()).stop();
                }
            });
            animationTimer.start();
        }
        else
        {
            Handler.currentGraphPanel.syncNodePositions();
        }

    }

    /**
     * An action listener for evaluation of user metrics and further execution of arrangement procedure
     */
    private class EvaluateAndArrange implements ActionListener
    {
        public EvaluateAndArrange()
		{
		}

		@Override
        public void actionPerformed(ActionEvent e)
        {
            executeLayout();
        }
    }

    public EvaluateAndArrange getEvaluateAndArrange() {return evaluateAndArrange;}
}
