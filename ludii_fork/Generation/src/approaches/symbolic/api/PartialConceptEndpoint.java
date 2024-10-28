package approaches.symbolic.api;

import approaches.symbolic.FractionalCompiler;
import approaches.symbolic.nodes.*;
import game.Game;
import game.equipment.component.Component;
import game.equipment.container.Container;
import game.rules.meta.MetaRule;
import game.rules.phase.Phase;
import game.rules.start.StartRule;
import game.types.board.SiteType;
import game.types.play.ModeType;
import graphics.ImageUtil;
import other.concept.Concept;
import other.concept.ConceptDataType;
import other.topology.TopologyElement;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static approaches.symbolic.FractionalCompiler.compileFraction;

public class PartialConceptEndpoint extends CachedEndpoint {
    static int maxConceptId = Arrays.stream(Concept.values()).max(Comparator.comparingInt(Concept::id)).get().id();

    public static void main(String[] args) {
        List<Concept> concepts = Arrays.stream(Concept.values()).sorted(Comparator.comparingInt(Concept::id)).toList();
        System.out.println(concepts.size() + " concepts vs " + maxConceptId + " max id");

        int previousId = 0;
        for (Concept concept: concepts) {
            assert concept.id() == previousId + 1;
            previousId = concept.id();
        }
//

        System.out.println(concepts.stream().map(c -> '"' + c.name()+ '"').toList());
        System.out.println(concepts.stream().map(Concept::id).toList());
        System.out.println(concepts.stream().map(c -> '"' + c.description().replaceAll("\"", "\\\\\"") + '"').toList());
        System.out.println(concepts.stream().map(c -> switch (c.dataType()) {
            case BooleanData -> "bool";
            case IntegerData -> "int";
            case StringData -> "str";
            case DoubleData -> "float";
        }).toList());

        new PartialConceptEndpoint().start();
    }

    @Override
    String cachedResponse() {
        FractionalCompiler.CompilationCheckpoint partialCompilation = compileFraction(standardInput, symbolMap);

        GameNode gameNode = partialCompilation.longest.get(0).consistentGame.root();

//        System.out.println(terminateClauses(gameNode));
//        System.out.println(gameNode.description());

        Game game = gameNode.safeInstantiate(symbolMap);
//        Game game = gameNode.nullInstantiate();
//
        game.equipment().createItems(game);

//        return toBinaryString(computeBooleanConcepts(game));
//        game.create();
        BitSet booleanConcepts = computeBooleanConcepts(game);

        System.out.println(booleanConcepts.length());

        return toBinaryString(booleanConcepts) + "|" + computeNonBooleanConcepts(game, booleanConcepts);
    }



    /**
     * @param bitSet bitset
     * @return "01010000" binary string
     */
    public static String toBinaryString(BitSet bitSet) {
        if (bitSet == null) {
            return null;
        }

        StringBuilder binaryString = new StringBuilder(IntStream.range(0, bitSet.length())
                .mapToObj(b -> String.valueOf(bitSet.get(b) ? 1 : 0))
                .collect(Collectors.joining()));

        // Pad with 0s to maxConceptId
        while (binaryString.length() < PartialConceptEndpoint.maxConceptId) {
            binaryString.append("0");
        }

        return binaryString.toString();
    }

    public BitSet computeBooleanConcepts(Game game)
    {
        final BitSet concept = new BitSet();

        try
        {
            // Accumulate concepts over the game.players().
            concept.or(game.players().concepts(game));

            // Accumulate concepts for all the containers.
            for (int i = 0; i < game.equipment().containers().length; i++)
                concept.or(game.equipment().containers()[i].concepts(game));

            // Accumulate concepts for all the components.
            for (int i = 1; i < game.equipment().components().length; i++)
                concept.or(game.equipment().components()[i].concepts(game));

            // Accumulate concepts for all the regions.
            for (int i = 0; i < game.equipment().regions().length; i++)
                concept.or(game.equipment().regions()[i].concepts(game));

            // Accumulate concepts for all the maps.
            for (int i = 0; i < game.equipment().maps().length; i++)
                concept.or(game.equipment().maps()[i].concepts(game));

            // Look if the game uses hints.
            if (game.equipment().vertexHints().length != 0 || game.equipment().cellsWithHints().length != 0
                    || game.equipment().edgesWithHints().length != 0)
                concept.set(Concept.Hints.id(), true);

            // Check if some regions are defined.
            if (game.equipment().regions().length != 0)
                concept.set(Concept.Region.id(), true);

            // We check if the owned pieces are asymmetric or not.
            final List<List<Component>> ownedPieces = new ArrayList<List<Component>>();
            for (int i = 0; i < game.players().count(); i++)
                ownedPieces.add(new ArrayList<Component>());

            // Check if the game has some asymmetric owned pieces.
            for (int i = 1; i < game.equipment().components().length; i++)
            {
                final Component component = game.equipment().components()[i];
                if (component.owner() > 0 && component.owner() <= game.players().count())
                    ownedPieces.get(component.owner() - 1).add(component);
            }

            if (!ownedPieces.isEmpty())
                for (final Component component : ownedPieces.get(0))
                {
                    final String nameComponent = component.getNameWithoutNumber();
                    final int owner = component.owner();
                    for (int i = 1; i < ownedPieces.size(); i++)
                    {
                        boolean found = false;
                        for (final Component otherComponent : ownedPieces.get(i))
                        {
                            if (otherComponent.owner() != owner
                                    && otherComponent.getNameWithoutNumber().equals(nameComponent))
                            {
                                found = true;
                                break;
                            }
                        }

                        if (!found)
                        {
                            concept.set(Concept.AsymmetricPiecesType.id(), true);
                            break;
                        }
                    }
                }

            // Accumulate concepts over meta rules
            if (game.rules().meta() != null)
                for (final MetaRule meta : game.rules().meta().rules())
                    concept.or(meta.concepts(game));

            // Accumulate concepts over starting rules
            if (game.rules().start() != null)
                for (final StartRule start : game.rules().start().rules())
                    concept.or(start.concepts(game));

            // Accumulate concepts over the playing game.rules().
            for (final Phase phase : game.rules().phases())
                concept.or(phase.concepts(game));

            // We check if the game has more than one phase.
            if (game.rules().phases().length > 1)
                concept.set(Concept.Phase.id(), true);

            // Accumulate concepts over the ending game.rules().
            if (game.rules().end() != null)
                concept.or(game.rules().end().concepts(game));

            concept.set(Concept.End.id(), true);

            // Look if the game uses a stack state.
            if (game.isStacking())
            {
                concept.set(Concept.StackState.id(), true);
                concept.set(Concept.Stack.id(), true);
            }

            // Look the graph element types used.
            concept.or(SiteType.concepts(game.board().defaultSite()));

            // Accumulate the stochastic concepts.
            if (concept.get(Concept.Dice.id()))
                concept.set(Concept.Stochastic.id(), true);
            if (concept.get(Concept.Domino.id()))
                concept.set(Concept.Stochastic.id(), true);
            if (concept.get(Concept.Card.id()))
                concept.set(Concept.Stochastic.id(), true);

            if (concept.get(Concept.Dice.id()) || concept.get(Concept.LargePiece.id()))
                concept.set(Concept.SiteState.id(), true);

            if (concept.get(Concept.LargePiece.id()))
                concept.set(Concept.SiteState.id(), true);

            if (concept.get(Concept.Domino.id()))
                concept.set(Concept.PieceCount.id(), true);

            for (int i = 1; i < game.equipment().components().length; i++)
            {
                final Component component = game.equipment().components()[i];
                if (component.getNameWithoutNumber() == null)
                    continue;
                final String componentName = component.getNameWithoutNumber().toLowerCase();
                if (componentName.equals("ball"))
                    concept.set(Concept.BallComponent.id(), true);
                else if (componentName.equals("disc"))
                    concept.set(Concept.DiscComponent.id(), true);
                else if (componentName.equals("marker"))
                    concept.set(Concept.MarkerComponent.id(), true);
                else if (componentName.equals("king") || componentName.equals("king_nocross"))
                    concept.set(Concept.KingComponent.id(), true);
                else if (componentName.equals("knight"))
                    concept.set(Concept.KnightComponent.id(), true);
                else if (componentName.equals("queen"))
                    concept.set(Concept.QueenComponent.id(), true);
                else if (componentName.equals("bishop") || componentName.equals("bishop_nocross"))
                    concept.set(Concept.BishopComponent.id(), true);
                else if (componentName.equals("rook"))
                    concept.set(Concept.RookComponent.id(), true);
                else if (componentName.equals("pawn"))
                    concept.set(Concept.PawnComponent.id(), true);
                else
                {
                    final String svgPath = ImageUtil.getImageFullPath(componentName);
                    if (svgPath == null) // The SVG can not be find.
                        continue;
                    if (svgPath.contains("tafl"))
                        concept.set(Concept.TaflComponent.id(), true);
                    else if (svgPath.contains("animal"))
                        concept.set(Concept.AnimalComponent.id(), true);
                    else if (svgPath.contains("fairyChess"))
                        concept.set(Concept.FairyChessComponent.id(), true);
                    else if (svgPath.contains("chess"))
                        concept.set(Concept.ChessComponent.id(), true);
                    else if (svgPath.contains("ploy"))
                        concept.set(Concept.PloyComponent.id(), true);
                    else if (svgPath.contains("shogi"))
                        concept.set(Concept.ShogiComponent.id(), true);
                    else if (svgPath.contains("xiangqi"))
                        concept.set(Concept.XiangqiComponent.id(), true);
                    else if (svgPath.contains("stratego"))
                        concept.set(Concept.StrategoComponent.id(), true);
                    else if (svgPath.contains("Janggi"))
                        concept.set(Concept.JanggiComponent.id(), true);
                    else if (svgPath.contains("hand"))
                        concept.set(Concept.HandComponent.id(), true);
                    else if (svgPath.contains("checkers"))
                        concept.set(Concept.CheckersComponent.id(), true);
                }
            }

            // Check the time model.
            if (game.mode().mode().equals(ModeType.Simulation))
                concept.set(Concept.Realtime.id(), true);
            else
                concept.set(Concept.Discrete.id(), true);

            // Check the mode.
            if (game.mode().mode().equals(ModeType.Alternating))
                concept.set(Concept.Alternating.id(), true);
            else if (game.mode().mode().equals(ModeType.Simultaneous))
                concept.set(Concept.Simultaneous.id(), true);
            else if (game.mode().mode().equals(ModeType.Simulation))
                concept.set(Concept.Simulation.id(), true);

            // Check the number of game.players().
            if (game.players().count() == 1)
            {
                concept.set(Concept.Solitaire.id(), true);
                if (!concept.get(Concept.DeductionPuzzle.id()))
                    concept.set(Concept.PlanningPuzzle.id(), true);
            }
            else if (game.players().count() == 2)
                concept.set(Concept.TwoPlayer.id(), true);
            else if (game.players().count() > 2)
                concept.set(Concept.Multiplayer.id(), true);

            // We put to true all the parents of concept which are true.
            for (final Concept possibleConcept : Concept.values())
                if (concept.get(possibleConcept.id()))
                {
                    Concept conceptToCheck = possibleConcept;
                    while (conceptToCheck != null)
                    {
                        if (conceptToCheck.dataType().equals(ConceptDataType.BooleanData))
                            concept.set(conceptToCheck.id(), true);
                        conceptToCheck = conceptToCheck.parent();
                    }
                }

            // Detection of some concepts based on the ludemeplexes used.
            for (String key: game.description().defineInstances().keySet()) {
                final String define = key.substring(1, key.length()-1);

                if(define.equals("AlquerqueBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.AlquerqueBoard.id(), true);

                if(define.equals("AlquerqueGraph") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.AlquerqueBoard.id(), true);

                if(define.equals("AlquerqueBoardWithBottomAndTopTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithTwoTriangles.id(), true);
                }

                if(define.equals("AlquerqueGraphWithBottomAndTopTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithTwoTriangles.id(), true);
                }

                if(define.equals("AlquerqueBoardWithBottomTriangle") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithOneTriangle.id(), true);
                }

                if(define.equals("AlquerqueGraphWithBottomTriangle") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithOneTriangle.id(), true);
                }

                if(define.equals("AlquerqueBoardWithFourTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithFourTriangles.id(), true);
                }

                if(define.equals("AlquerqueGraphWithFourTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithFourTriangles.id(), true);
                }

                if(define.equals("AlquerqueBoardWithEightTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.AlquerqueBoard.id(), true);
                    concept.set(Concept.AlquerqueBoardWithEightTriangles.id(), true);
                }

                if(define.equals("ThreeMensMorrisBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.ThreeMensMorrisBoard.id(), true);

                if(define.equals("ThreeMensMorrisBoardWithLeftAndRightTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.ThreeMensMorrisBoard.id(), true);
                    concept.set(Concept.ThreeMensMorrisBoardWithTwoTriangles.id(), true);
                }

                if(define.equals("ThreeMensMorrisGraphWithLeftAndRightTriangles") && game.description().defineInstances().get(key).define().isKnown())
                {
                    concept.set(Concept.ThreeMensMorrisBoard.id(), true);
                    concept.set(Concept.ThreeMensMorrisBoardWithTwoTriangles.id(), true);
                }

                if(define.equals("NineMensMorrisBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.NineMensMorrisBoard.id(), true);

                if(define.equals("StarBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.StarBoard.id(), true);

                if(define.equals("CrossBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.CrossBoard.id(), true);

                if(define.equals("CrossGraph") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.CrossBoard.id(), true);

                if(define.equals("KintsBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.KintsBoard.id(), true);

                if(define.equals("PachisiBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.PachisiBoard.id(), true);

                if(define.equals("FortyStonesWithFourGapsBoard") && game.description().defineInstances().get(key).define().isKnown())
                    concept.set(Concept.FortyStonesWithFourGapsBoard.id(), true);

            }
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }

        return concept;
    }


    public Map<Integer, String> computeNonBooleanConcepts(Game game, BitSet booleanConcepts)
    {
        final Map<Integer, String> nonBooleanConcepts = new HashMap<Integer, String>();

        // Compute the average number of each absolute direction.
        final SiteType defaultSiteType = game.board().defaultSite();
        final List<? extends TopologyElement> elements = game.board().topology().getGraphElements(defaultSiteType);
        final int numDefaultElements = elements.size();
        int totalNumDirections = 0;
        int totalNumOrthogonalDirections = 0;
        int totalNumDiagonalDirections = 0;
        int totalNumAdjacentDirections = 0;
        int totalNumOffDiagonalDirections = 0;
        for (final TopologyElement element : elements)
        {
            totalNumDirections += element.neighbours().size();
            totalNumOrthogonalDirections += element.orthogonal().size();
            totalNumDiagonalDirections += element.diagonal().size();
            totalNumAdjacentDirections += element.adjacent().size();
            totalNumOffDiagonalDirections += element.off().size();
        }
        String avgNumDirection = new DecimalFormat("##.##").format((double) totalNumDirections / (double) numDefaultElements) + "";
        avgNumDirection = avgNumDirection.replaceAll(",", ".");

        String avgNumOrthogonalDirection = new DecimalFormat("##.##").format((double) totalNumOrthogonalDirections / (double) numDefaultElements) + "";
        avgNumOrthogonalDirection = avgNumOrthogonalDirection.replaceAll(",", ".");

        String avgNumDiagonalDirection = new DecimalFormat("##.##").format((double) totalNumDiagonalDirections / (double) numDefaultElements) + "";
        avgNumDiagonalDirection = avgNumDiagonalDirection.replaceAll(",", ".");

        String avgNumAdjacentlDirection = new DecimalFormat("##.##").format((double) totalNumAdjacentDirections / (double) numDefaultElements) + "";
        avgNumAdjacentlDirection = avgNumAdjacentlDirection.replaceAll(",", ".");

        String avgNumOffDiagonalDirection = new DecimalFormat("##.##").format((double) totalNumOffDiagonalDirections / (double) numDefaultElements) + "";
        avgNumOffDiagonalDirection = avgNumOffDiagonalDirection.replaceAll(",", ".");

        for (final Concept concept : Concept.values())
            if (!concept.dataType().equals(ConceptDataType.BooleanData))
            {
                switch (concept)
                {
                    case NumPlayableSites:
                        int countPlayableSites = 0;
                        for (int cid = 0; cid < game.equipment().containers().length; cid++)
                        {
                            final Container container = game.equipment().containers()[cid];
                            if (cid != 0)
                                countPlayableSites += container.numSites();
                            else
                            {
                                if (booleanConcepts.get(Concept.Cell.id()))
                                    countPlayableSites += container.topology().cells().size();

                                if (booleanConcepts.get(Concept.Vertex.id()))
                                    countPlayableSites += container.topology().vertices().size();

                                if (booleanConcepts.get(Concept.Edge.id()))
                                    countPlayableSites += container.topology().edges().size();
                            }
                        }
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), countPlayableSites + "");
                        break;
                    case NumPlayableSitesOnBoard:
                        int countPlayableSitesOnBoard = 0;
                        final Container container = game.equipment().containers()[0];
                        if (booleanConcepts.get(Concept.Cell.id()))
                            countPlayableSitesOnBoard += container.topology().cells().size();

                        if (booleanConcepts.get(Concept.Vertex.id()))
                            countPlayableSitesOnBoard += container.topology().vertices().size();

                        if (booleanConcepts.get(Concept.Edge.id()))
                            countPlayableSitesOnBoard += container.topology().edges().size();
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), countPlayableSitesOnBoard + "");
                        break;
                    case NumPlayers:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.players().count() + "");
                        break;
                    case NumColumns:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().columns(defaultSiteType).size() + "");
                        break;
                    case NumRows:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().rows(defaultSiteType).size() + "");
                        break;
                    case NumCorners:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().corners(defaultSiteType).size() + "");
                        break;
                    case NumDirections:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumDirection);
                        break;
                    case NumOrthogonalDirections:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumOrthogonalDirection);
                        break;
                    case NumDiagonalDirections:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumDiagonalDirection);
                        break;
                    case NumAdjacentDirections:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumAdjacentlDirection);
                        break;
                    case NumOffDiagonalDirections:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumOffDiagonalDirection);
                        break;
                    case NumOuterSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().outer(defaultSiteType).size() + "");
                        break;
                    case NumInnerSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().inner(defaultSiteType).size() + "");
                        break;
                    case NumLayers:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().layers(defaultSiteType).size() + "");
                        break;
                    case NumEdges:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.board().topology().edges().size() + "");
                        break;
                    case NumCells:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.board().topology().cells().size() + "");
                        break;
                    case NumVertices:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.board().topology().vertices().size() + "");
                        break;
                    case NumPerimeterSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().perimeter(defaultSiteType).size() + "");
                        break;
                    case NumTopSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().top(defaultSiteType).size() + "");
                        break;
                    case NumBottomSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().bottom(defaultSiteType).size() + "");
                        break;
                    case NumRightSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().right(defaultSiteType).size() + "");
                        break;
                    case NumLeftSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().left(defaultSiteType).size() + "");
                        break;
                    case NumCentreSites:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().centre(defaultSiteType).size() + "");
                        break;
                    case NumConvexCorners:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().cornersConvex(defaultSiteType).size() + "");
                        break;
                    case NumConcaveCorners:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()),
                                game.board().topology().cornersConcave(defaultSiteType).size() + "");
                        break;
                    case NumPhasesBoard:
                        int numPhases = 0;
                        final List<List<TopologyElement>> phaseElements = game.board().topology().phases(defaultSiteType);
                        for (final List<TopologyElement> topoElements : phaseElements)
                            if (topoElements.size() != 0)
                                numPhases++;
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), numPhases + "");
                        break;
                    case NumComponentsType:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.equipment().components().length - 1 + "");
                        break;
                    case NumComponentsTypePerPlayer:
                        final int[] componentsPerPlayer = new int[game.players().size()];
                        for (int i = 1; i < game.equipment().components().length; i++)
                        {
                            final Component component = game.equipment().components()[i];
                            if (component.owner() > 0 && component.owner() < game.players().size())
                                componentsPerPlayer[component.owner()]++;
                        }
                        int numOwnerComponent = 0;
                        for (int i = 1; i < componentsPerPlayer.length; i++)
                            numOwnerComponent += componentsPerPlayer[i];
                        String avgNumComponentPerPlayer = game.players().count() <= 0 ? "0" : new DecimalFormat("##.##")
                                .format((double) numOwnerComponent / (double) game.players().count()) + "";
                        avgNumComponentPerPlayer = avgNumComponentPerPlayer.replaceAll(",", ".");
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), avgNumComponentPerPlayer);
                        break;
                    case NumPlayPhase:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.rules().phases().length + "");
                        break;
                    case NumDice:
                        int numDice = 0;
                        for (int i = 1; i < game.equipment().components().length; i++)
                            if (game.equipment().components()[i].isDie())
                                numDice++;
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), numDice + "");
                        break;
                    case NumContainers:
                        nonBooleanConcepts.put(Integer.valueOf(concept.id()), game.equipment().containers().length + "");
                        break;
                    default:
                        break;
                }
            }

        return nonBooleanConcepts;
    }
}