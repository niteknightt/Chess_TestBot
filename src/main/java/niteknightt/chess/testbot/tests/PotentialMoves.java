package niteknightt.chess.testbot.tests;

import niteknightt.chess.testbot.EvaluatedMove;

import java.util.ArrayList;
import java.util.List;

public class PotentialMoves {
    public List<EvaluatedMove> evaluatedMoves = new ArrayList<>();
    public int numVeryMuchBetterMoves = 0;
    public int numMuchBetterMoves = 0;
    public int numSomewhatBetterMoves = 0;
    public int numSameMoves = 0;
    public int numVeryMuchWorseMoves = 0;
    public int numMuchWorseMoves = 0;
    public int numSomewhatWorseMoves = 0;

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
            case VERY_MUCH_BETTER_THAN_BEFORE:
                ++numVeryMuchBetterMoves;
                break;
            case MUCH_BETTER_THAN_BEFORE:
                ++numMuchBetterMoves;
                break;
            case SOMEWHAT_BETTER_THAN_BEFORE:
                ++numSomewhatBetterMoves;
                break;
            case SAME_AS_BEFORE:
                ++numSameMoves;
                break;
            case VERY_MUCH_WORSE_THAN_BEFORE:
                ++numVeryMuchWorseMoves;
                break;
            case MUCH_WORSE_THAN_BEFORE:
                ++numMuchWorseMoves;
                break;
            case SOMEWHAT_WORSE_THAN_BEFORE:
                ++numSomewhatWorseMoves;
                break;
            case NONE:
            default:
                throw new RuntimeException("Got evaluated move without category");
        }
    }
}
