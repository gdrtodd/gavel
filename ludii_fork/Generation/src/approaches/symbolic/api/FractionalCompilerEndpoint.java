package approaches.symbolic.api;

import approaches.symbolic.nodes.GameNode;
import game.Game;
import supplementary.experiments.eval.EvalGames;

import java.util.List;
import java.util.stream.Collectors;

import static approaches.symbolic.FractionalCompiler.*;

/*
    * Used by the extension to evaluate up to what point a game description compiles.
    * It does not yet support definitions. But it can tell you where the error is.
    *
    * @author Alexander Padula
 */
public class FractionalCompilerEndpoint extends CachedEndpoint {

    public static void main(String[] args) {
        new FractionalCompilerEndpoint().start();
    }

    @Override
    String cachedResponse() {
        CompilationCheckpoint partialCompilation = compileFraction(standardInput, symbolMap);

        GameNode gameNode = partialCompilation.longest.get(0).consistentGame.root();
        String compilingPortion = gameNode.description();
        boolean compiles = partialCompilation.longest.get(0).exceptions.isEmpty() && gameNode.isRecursivelyComplete();
        double eval = 0;
        Game game;
        List<String> willCrash;
        List<String> missingRequirements = null;

        game = gameNode.safeInstantiate(symbolMap);


        game.create();

//        System.out.println(game.computeRequirementReport());
//        System.out.println(game.computeCrashReport());
        missingRequirements = game.requirementReport();
        willCrash = game.crashReport();
        try {
            eval = EvalGames.defaultEvaluationFast(gameNode.instantiate());
        } catch (Exception internalError) {
            eval = 0;
//            internalError.printStackTrace();
        }
        //(game "Chess" (players {(player N) (player S)}) (equipment {(board (square 8)) (piece "Rook" Each (move Slide Orthogonal (to if:(is Enemy (who at:(to))) (apply (remove (to) (then (set Counter))))) (then (if (= (state at:(last To)) 1) (set State at:(last To) 0))))) (piece "King" Each (move Step (to if:(not (is Friend (who at:(to)))) (apply (if (is Enemy (who at:(to))) (remove (to) (then (set Counter)))))) (then (if (= (state at:(last To)) 1) (set State at:(last To) 0))))) (piece "Bishop" Each (move Slide Diagonal (to if:(is Enemy (who at:(to))) (apply (remove (to) (then (set Counter))))))) (piece "Knight" Each (move Leap {{F F R F} {F F L F}} (to if:(not (is Friend (who at:(to)))) (apply (if (is Enemy (who at:(to))) (remove (to) (then (set Counter)))))))) (piece "Queen" Each (move Slide (to if:(is Enemy (who at:(to))) (apply (remove (to) (then (set Counter))))))) (map "King" {(pair 1 "E1") (pair 2 "E8")}) (map "RookLeft" {(pair 1 "A1") (pair 2 "A8")}) (map "RookRight" {(pair 1 "H1") (pair 2 "H8")}) (regions "Promotion" P1 (sites Top)) (regions "Promotion" P2 (sites Bottom))}) (rules (start {(place "Pawn1" (sites Row 1)) (place "Pawn2" (sites Row 6)) (place "Rook1" {"A1" "H1"} state:1) (place "Knight1" {"B1" "G1"}) (place "Bishop1" {"C1" "F1"}) (place "Queen1" coord:"D1") (place "King1" coord:"E1" state:1) (place "Rook2" {"A8" "H8"} state:1) (place "Knight2" {"B8" "G8"}) (place "Bishop2" {"C8" "F8"}) (place "Queen2" coord:"D8") (place "King2" coord:"E8" state:1)}) (play (if (is Prev Mover) (move Promote (last To) (piece {"Queen" "Knight" "Bishop" "Rook"}) Mover) (do (or (forEach Piece) (if (and (= (state at:(mapEntry "King" (mover))) 1) (not (is Threatened (id "King" Mover)))) (or (if (and (= (state at:(mapEntry "RookLeft" (mover))) 1) (can Move (slide (from (mapEntry "RookLeft" (mover))) E (between (exact 3) if:(is Empty (to))) (to if:True (apply (set State at:(from) 0)))))) (move Slide (from (mapEntry "King" (mover))) W (between (exact 2) if:(and (is Empty (to)) (not (is Threatened (id "King" Mover) at:(to))))) (to if:True (apply (set State at:(from) 0))) (then (slide (from (mapEntry "RookLeft" (mover))) E (between (exact 3) if:True) (to if:True (apply (set State at:(from) 0))))))) (if (and (= (state at:(mapEntry "RookRight" (mover))) 1) (can Move (slide (from (mapEntry "RookRight" (mover))) W (between (exact 2) if:(is Empty (to))) (to if:True (apply (set State at:(from) 0)))))) (move Slide (from (mapEntry "King" (mover))) E (between (exact 2) if:(and (is Empty (to)) (not (is Threatened (id "King" Mover) at:(to))))) (to if:True (apply (set State at:(from) 0))) (then (slide (from (mapEntry "RookRight" (mover))) W (between (exact 2) if:True) (to if:True (apply (set State at:(from) 0)))))))))) ifAfterwards:(not (is Threatened (id "King" Mover)))))) (end {(if (and (is Threatened (id "King" Next)) (not (can Move (do (forEach Piece Next) ifAfterwards:(not (is Threatened (id "King" Next))))))) (result Mover Win)) (if (or (no Moves Mover) (= (counter) 99)) (result Mover Draw))})))
        //(game "Dice Chess" (players {(player N) (player S)}) (equipment {(board (square 8)) (piece "Pawn" Each (or {(if (is In (from) (sites Start (piece (what at:(from))))) (move Slide Forward (between (exact 2) if:(is Empty (between))) (to if:(is Empty (to))))) (move Step Forward (to if:(is Empty (to)))) (move Step (directions {FR FL}) (to if:(is Enemy (who at:(to))) (apply (remove (to)))))} (then (if (is In (last To) (sites Mover "Promotion")) (moveAgain))))) (piece "Bishop" Each (move Slide Diagonal (to if:(is Enemy (who at:(to))) (apply (remove (to)))))) (piece "Knight" Each (move Leap {{F F R F} {F F L F}} (to if:(not (is Friend (who at:(to)))) (apply (if (is Enemy (who at:(to))) (remove (to))))))) (piece "Rook" Each (move Slide Orthogonal (to if:(is Enemy (who at:(to))) (apply (remove (to)))))) (piece "Queen" Each (move Slide (to if:(is Enemy (who at:(to))) (apply (remove (to)))))) (piece "King_noCross" Each (move Step (to if:(not (is Friend (who at:(to)))) (apply (if (is Enemy (who at:(to))) (remove (to))))))) (regions "Promotion" P1 (sites Top)) (regions "Promotion" P2 (sites Bottom))}) (rules (start {(place "Pawn1" {"A2" "B2" "C2" "D2" "E2" "F2" "G2" "H2"}) (place "Pawn2" {"H7" "G7" "E7" "F7" "D7" "C7" "B7" "A7"}) (place "Bishop1" {"C1" "F1"}) (place "Bishop2" {"C8" "F8"}) (place "Knight1" {"B1" "G1"}) (place "Knight2" {"G8" "B8"}) (place "Rook1" {"A1" "H1"}) (place "Rook2" {"H8" "A8"}) (place "Queen1" coord:"D1") (place "Queen2" coord:"D8") (place "King_noCross1" coord:"E1") (place "King_noCross2" coord:"E8")}) (play (do (if (not (is Prev Mover)) (roll)) next:(if (is Prev Mover) (if (= (count Pips) 1) (or {(move Promote (last To) (piece "Bishop") Mover) (move Promote (last To) (piece "Knight") Mover) (move Promote (last To) (piece "Rook") Mover) (move Promote (last To) (piece "Queen") Mover) (move Promote (last To) (piece "King_noCross") Mover)}) (if (= (count Pips) 2) (move Promote (last To) (piece "Knight") Mover) (if (= (count Pips) 3) (move Promote (last To) (piece "Bishop") Mover) (if (= (count Pips) 4) (move Promote (last To) (piece "Rook") Mover) (if (= (count Pips) 5) (move Promote (last To) (piece "Queen") Mover) (move Promote (last To) (piece "King_noCross") Mover)))))) (or (forEach Die (if (= (pips) 6) (forEach Piece "King_noCross") (if (= (pips) 5) (forEach Piece "Queen") (if (= (pips) 4) (forEach Piece "Rook") (if (= (pips) 2) (forEach Piece "Knight") (if (= (pips) 3) (forEach Piece "Bishop") (if (= (pips) 1) (forEach Piece "Pawn")))))))) (forEach Site (sites Direction from:(sites Mover) (if (is Mover P1) S N) distance:1) (if (= (what at:(site)) (id "Pawn" Mover)) (or (if (can Move (move Step (from (site)) (directions {FR FL}) (to if:(is Enemy (who at:(to))) (apply (remove (to)))))) (move Step (from (site)) (directions {FR FL}) (to if:(is Enemy (who at:(to))) (apply (remove (to)))))) (if (can Move (move Step (from (site)) Forward (to if:(is Empty (to))))) (move Step (from (site)) Forward (to if:(is Empty (to))))) (then (if (is In (last To) (sites Mover "Promotion")) (moveAgain)))))))))) (end (if (no Pieces Next "King_noCross") (result Next Loss)))))

//        System.out.println("Compiles: " + compiles);
//        System.out.println("Eval: " + eval);
//        System.out.println("Missing Requirements: " + missingRequirements);
//        System.out.println("Will Crash: " + willCrash);

        return (compiles ? "1":"0") + "||" + eval + "||" + destandardize(rawInput, compilingPortion) + "||" + String.join("|", missingRequirements) + "||" + String.join(",", willCrash);
    }
}
