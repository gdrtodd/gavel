package app.display.dialogs.visual_editor.model;


import app.display.dialogs.visual_editor.handler.Handler;
import app.display.dialogs.visual_editor.model.interfaces.iGNode;
import app.display.dialogs.visual_editor.model.interfaces.iGraph;
import main.grammar.Description;
import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Graph of LudemeNode objects
 * @author Filipp Dokienko
 */

public class DescriptionGraph implements iGraph
{
    /** The Root node of this graph */
    private LudemeNode ROOT;
    /** All nodes this graph contains keyed by their id */
    private final HashMap<Integer, iGNode> nodeMap = new HashMap<>();
    /** List of all LudemeNodes this graph contains */
    private final List<LudemeNode> allLudemeNodes = new ArrayList<>();
    /** List of all Edges this graph contains */
    private final List<Edge> edgeList = new ArrayList<>();

    private final List<Integer> connectedComponentRoots = new ArrayList<>();
    private int selectedRoot = -1;

    /** Whether this graph is a define */
    private boolean isDefine = false;
    /** The title of the define */
    private String title;
    /** The symbol of the define */
    private Symbol defineSymbol;
    /** The macro node of the define */
    private LudemeNode defineMacroNode;
    /** The parameters of the define */
    private List<NodeArgument> defineParameters = new ArrayList<>();

    /**
     * Constructor. Used for non-define graphs without a predefined root node
     */
    public DescriptionGraph()
    {

    }

    /**
     * Constructor for a define graph.
     * @param title Initial title of the define
     * @param isDefine Whether this graph is a define
     */
    public DescriptionGraph(String title, boolean isDefine)
    {
        assert isDefine;
        this.title = title;
        this.isDefine = true;
        this.defineSymbol = new Symbol(Symbol.LudemeType.Ludeme, title, title, null);
    }

    /**
     * Constructor for a non-define graph with a predefined root node
     * @param root The root node
     */
    public DescriptionGraph(LudemeNode root)
    {
        this.ROOT = root;
    }

    public Description description()
    {
        return new Description(ROOT.toLud());
    }

    @Override
    public List<Edge> getEdgeList()
    {
        return edgeList;
    }

    @Override
    public HashMap<Integer, iGNode> getNodeList()
    {
        return nodeMap;
    }

	@Override
    public LudemeNode getNode(int id)
    {
        return (LudemeNode) nodeMap.get(Integer.valueOf(id));
    }

    public List<LudemeNode> getNodes()
    {
        return allLudemeNodes;
    }

	@Override
    public int addNode(iGNode ludemeNode)
    {
        this.allLudemeNodes.add((LudemeNode) ludemeNode);
        int id = ludemeNode.id();
        nodeMap.put(Integer.valueOf(id), ludemeNode);
        addConnectedComponentRoot(id);
        return id;
    }

    @Override
    public void removeNode(iGNode node)
    {
        this.allLudemeNodes.remove(node);
        nodeMap.remove(Integer.valueOf(node.id()));
        removeConnectedComponentRoot(node.id());
        node.id();
    }

    @Override
    public void addEdge(int from, int to)
    {
        Edge e = new Edge(from, to);
        for(Edge edge : edgeList)
            if(edge.equals(e))
                return;
        edgeList.add(new Edge(from , to));
        removeConnectedComponentRoot(to);
    }

    @Override
    public void removeEdge(int from, int to)
    {
        for(Edge e : edgeList)
            if(e.getNodeA() == from && e.getNodeB() == to)
            {
                edgeList.remove(e);
                addConnectedComponentRoot(to);
                return;
            }
    }

    @Override
    public List<Integer> connectedComponentRoots()
    {
        return connectedComponentRoots;
    }

    @Override
    public void addConnectedComponentRoot(int root)
    {
        if (!connectedComponentRoots.contains(Integer.valueOf(root)) && getNode(root) != null)
            connectedComponentRoots.add(Integer.valueOf(root));
    }

    @Override
    public void removeConnectedComponentRoot(int root)
    {
        if (connectedComponentRoots.contains(Integer.valueOf(root)))
            connectedComponentRoots.remove(Integer.valueOf(root));
    }

    @Override
    public int selectedRoot()
    {
        return selectedRoot;
    }

    @Override
    public void setSelectedRoot(int root)
    {
        this.selectedRoot = root;
    }

    @Override
    public void setRoot(iGNode root)
    {
        this.ROOT = (LudemeNode) root;
    }

    @Override
    public iGNode getRoot()
    {
        return ROOT;
    }

    public LudemeNode rootLudemeNode()
    {
        return ROOT;
    }

    public void remove(LudemeNode ludemeNode)
    {
        this.allLudemeNodes.remove(ludemeNode);
    }

    /**
     * Converts the graph to a .lud String
     * @return The .lud equivalent of the graph
     */
    public String toLud()
    {
        if(!isDefine)
            return ROOT.toLud();
        else
            return defineLud();
    }

    // Methods for define graphs


    /**
     * Converts a define graph to a .lud String.
     * Replaces unprovided, but required arguments, indicated by " <PARAMETER> " with #[count]
     * @return The .lud equivalent of the define graph
     */
    private String defineLud()
    {
        String lud = ((LudemeNode) getRoot()).toLud(true);
        int count = 1;
        while(lud.contains("<PARAMETER>"))
        {
            lud = lud.replaceFirst("<PARAMETER>","#"+count);
            count++;
        }
        return lud;
    }

    /**
     *
     * @return Whether this is a define graph
     */
    public boolean isDefine()
    {
        return isDefine;
    }

    /**
     *
     * @return The title of the define
     */
    public String title()
    {
        return title;
    }

    /**
     * Changes the title of the define
     * @param title The new title
     */
    public void setTitle(String title)
    {
        if(!title.equals(this.title))
        {
            this.title = title;
            // update the symbol
            this.defineSymbol = new Symbol(Symbol.LudemeType.Ludeme, title, title, null);
            // notify handler
            Handler.updateDefineNodes(this, defineSymbol);
        }
    }

    /**
     *
     * @return The symbol of the define
     */
    public Symbol defineSymbol()
    {
        return defineSymbol;
    }

    /**
     *
     * @return The macro node (root of the define (excluding the define node itself)) of the define
     */
    public LudemeNode defineMacroNode()
    {
        return defineMacroNode;
    }

    /**
     *
     * @return The current macro node of the define
     */
    public LudemeNode computeDefineMacroNode()
    {
        return (LudemeNode) rootLudemeNode().providedInputsMap().get(rootLudemeNode().currentNodeArguments().get(1));
    }

    /**
     * Updates the field of the define macro node
     */
    public void updateMacroNode()
    {
        // if the macro node has changed, update it and notify about change
        if(defineMacroNode != computeDefineMacroNode())
        {
            defineMacroNode = computeDefineMacroNode();
            updateParameters(computeDefineParameters());
            Handler.updateDefineNodes(this, defineMacroNode);
        }
    }

    public List<NodeArgument> parameters()
    {
        if(defineParameters == null)
            defineParameters = computeDefineParameters();
        return defineParameters;
    }

    /**
     * Computes the required arguments of the define
     * @return List of NodeArguments that require an Input
     */
    public List<NodeArgument> computeDefineParameters()
    {
        assert isDefine;
        List<NodeArgument> parameters = new ArrayList<>();
        LudemeNode root = (LudemeNode) getRoot();
        // The node must be satisfied (means all inputs filled)
        if(!root.isSatisfied())
            return parameters;

        for(LudemeNode node : allLudemeNodes)
            if(Handler.isConnectedToRoot(this, node))
                parameters.addAll(node.unfilledRequiredArguments());

        return parameters;
    }

    /**
     * Updates the current parameters of the define.
     * computes which NodeArguments were added/removed from the parameters (of a define node)
     */
    public void updateParameters(List<NodeArgument> parameters)
    {
        assert isDefine;
        this.defineParameters = parameters;
        Handler.updateDefineNodes(this, parameters);
    }

    private NodeArgument titleNodeArgument()
    {
        return rootLudemeNode().currentNodeArguments().get(0);
    }

    private NodeArgument macroNodeArgument()
    {
        return rootLudemeNode().currentNodeArguments().get(1);

    }

    public void notifyDefineChanged(NodeArgument nodeArgument, Object input)
    {
        // check what changed
        if(nodeArgument == titleNodeArgument() && !input.equals(""))
            setTitle((String) input);
        else if(nodeArgument == macroNodeArgument())
            updateMacroNode();
        else
            updateParameters(computeDefineParameters());
    }

    /**
     *
     * @return The node of the define, used in other graphs
     */
    public LudemeNode defineNode()
    {
        assert isDefine;
        if(defineMacroNode() == null || parameters() == null)
            return null;
        return new LudemeNode(defineSymbol, defineMacroNode(), this, parameters(), rootLudemeNode().x(), rootLudemeNode().y(), true);
    }
}
