package niteknightt.chess.testbot;

import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.testbot.EvaluatedMove;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PotentialMoves {
    public List<EvaluatedMove> evaluatedMoves = new ArrayList<>();
    protected Board _board;
    public int numWinning = 0;
    public int numWellAhead = 0;
    public int numLeading = 0;
    public int numEqual = 0;
    public int numLagging = 0;
    public int numWellBehind = 0;
    public int numLosing = 0;

    public PotentialMoves(List<EvaluatedMove> moves, Board board) {
        _board = board;
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

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        DecimalFormat fmt = new DecimalFormat("+00.00;-00.00");
        for (int i = 0; i < evaluatedMoves.size(); ++i) {
            Move move = new Move(evaluatedMoves.get(i).uci, _board);
            sb.append("{");
            sb.append(String.format("%02d", i));
            sb.append(" ");
            sb.append(move.algebraicFormat());
            sb.append(" ");
            sb.append(fmt.format(evaluatedMoves.get(i).eval));
            sb.append(" ");
            sb.append(evaluatedMoves.get(i).evalCategory);
            sb.append("}");
            if (i < evaluatedMoves.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }
}
