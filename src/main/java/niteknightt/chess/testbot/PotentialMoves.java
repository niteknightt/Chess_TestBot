package niteknightt.chess.testbot;

import niteknightt.chess.testbot.EvaluatedMove;

import java.util.ArrayList;
import java.util.List;

public class PotentialMoves {
    public List<EvaluatedMove> evaluatedMoves = new ArrayList<>();
    public int numWinning = 0;
    public int numWellAhead = 0;
    public int numLeading = 0;
    public int numEqual = 0;
    public int numLagging = 0;
    public int numWellBehind = 0;
    public int numLosing = 0;

    public PotentialMoves(List<EvaluatedMove> moves) {
        if (moves == null) {
            return;
        }

        for (EvaluatedMove move : moves) {
            addMove(move);
        }
    }

    public void addMove(EvaluatedMove move) {
        evaluatedMoves.add(move);

        switch (move.evalCategory) {
            case WINNING:
                ++numWinning;
                break;
            case WELL_AHEAD:
                ++numWellAhead;
                break;
            case LEADING:
                ++numLeading;
                break;
            case EQUAL:
                ++numEqual;
                break;
            case LAGGING:
                ++numLagging;
                break;
            case WELL_BEHIND:
                ++numWellBehind;
                break;
            case LOSING:
                ++numLosing;
                break;
            case NONE:
            default:
                throw new RuntimeException("Got evaluated move without category");
        }
    }
}
