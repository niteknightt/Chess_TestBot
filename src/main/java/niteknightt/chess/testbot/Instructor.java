package niteknightt.chess.testbot;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessInterface;
import niteknightt.chess.testbot.MoveWithEval;

import java.util.List;

public class Instructor {

    public static double BORDER_BETWEEN_MUCH_BETTER_AND_A_LITTLE_BETTER = 1.0;
    public static double BORDER_BETWEEN_A_LITTLE_BETTER_AND_BASICALLY_EQUAL = 0.25;

    public static boolean reviewLastHumanMove(BotGameVsHuman game) {
        // Must have:
        //   * List of moves that human had available with eval enum
        //   * Which move human made
        // Get:
        //   * List of moves computer now has available with eval enum
        //
        // Action:
        //   * If computer has 2 or more moves which are MUCH_BETTER_THAN_BEFORE.
        //   * if computer has 1 move MUCH_BETTER_THAN_BEFORE and 1 or more moves
        //     that are SOMEWHAT_BETTER_THAN_BEFORE
        //   * If computer has 1 move MUCH_BETTER_THAN_BEFORE and all others
        //     are SAME_AS_BEFORE or below.
        //   * If computer has 0 moves MUCH_BETTER_THAN_BEFORE, but 2 or more moves
        //     which are SOMEWHAT_BETTER_THAN_BEFORE.
        //   * If computer has 0 moves MUCH_BETTER_THAN_BEFORE, but 1 move which is
        //     SOMEWHAT_BETTER_THAN_BEFORE.
        //   * If computer has 0 moves MUCH_BETTER_THAN_BEFORE, and 0 moves which
        //     are SOMEWHAT_BETTER_THAN_BEFORE.
        List<MoveWithEval> movesAvailableForChallenger = null;

        Board gameBoardClone;
        Move lastMove;

        try {
            gameBoardClone = game.getBoard().clone();
            lastMove = game.getLastMove();
            gameBoardClone.undoMoveForGame(lastMove);
            movesAvailableForChallenger = game.getMoveSelector().getAllMoves(gameBoardClone);
        }
        catch (Exception e) {
            AppLogger.getInstance().error("Failed to get available moves for challenger");
            return false;
        }

        int numMovesBetter = 0;
        int numMovesMuchBetter = 0;
        int numMovesALittleBetter = 0;
        int numMovesBasicallyEqual = 0;
        int numMovesEqualOrWorse = 0;

        int challengerMoveIndex = -1;

        // First find the challenger move and eval in the list of legal moves.
        for (int i = 0; i < movesAvailableForChallenger.size(); ++i) {
            MoveWithEval availableMove = movesAvailableForChallenger.get(i);
            if (availableMove.uci.equals(lastMove.uciFormat())) {
                challengerMoveIndex = i;
                break;
            }
        }

        // Handle when the move is not found.
        if (challengerMoveIndex == -1) {
            AppLogger.getInstance().error("Failed to find challenger move in list of available moves");
            return false;
        }

        MoveWithEval challengerMoveWithEval = movesAvailableForChallenger.get(challengerMoveIndex);
        boolean bestMove = false;

        if (challengerMoveIndex == 0) {
            bestMove = true;
        }
        else {
            numMovesBetter = challengerMoveIndex;
            for (int i = 0; i < numMovesBetter; ++i) {
                MoveWithEval availableMove = movesAvailableForChallenger.get(i);
                double evalDiff = Math.abs(availableMove.eval - challengerMoveWithEval.eval);
                if (evalDiff > BORDER_BETWEEN_MUCH_BETTER_AND_A_LITTLE_BETTER) {
                    ++numMovesMuchBetter;
                }
                else if (evalDiff > BORDER_BETWEEN_A_LITTLE_BETTER_AND_BASICALLY_EQUAL) {
                    ++numMovesALittleBetter;
                }
                else {
                    ++numMovesBasicallyEqual;
                }
            }
            numMovesEqualOrWorse = movesAvailableForChallenger.size() - numMovesBetter;
        }

        StringBuilder sb = new StringBuilder();
        if (bestMove) {
            sb.append("That was the best move!");
        }
        else if (numMovesMuchBetter == 1) {
            sb.append("There was a much better move you could have made (" + new Move(movesAvailableForChallenger.get(0).uci, gameBoardClone).algebraicFormat() + ").");
            if (challengerMoveWithEval.continuation.length > 0) {
                Move move = new Move(challengerMoveWithEval.uci, gameBoardClone);
                gameBoardClone.handleMoveForGame(move);
                sb.append(" I might now play " + new Move(challengerMoveWithEval.continuation[0], gameBoardClone).algebraicFormat());
                gameBoardClone.undoSingleAnalysisMove(move);
            }
        }
        else if (numMovesMuchBetter > 1) {
            sb.append("There were much better moves you could have made (such as " + new Move(movesAvailableForChallenger.get(0).uci, gameBoardClone).algebraicFormat() + ").");
            if (challengerMoveWithEval.continuation.length > 0) {
                Move move = new Move(challengerMoveWithEval.uci, gameBoardClone);
                gameBoardClone.handleMoveForGame(move);
                sb.append(" I might now play " + new Move(challengerMoveWithEval.continuation[0], gameBoardClone).algebraicFormat());
                gameBoardClone.undoSingleAnalysisMove(move);
            }
        }
        else  if (numMovesALittleBetter == 1) {
            sb.append("There was a slightly better move you could have made (" + new Move(movesAvailableForChallenger.get(0).uci, gameBoardClone).algebraicFormat() + ").");
        }
        else if (numMovesALittleBetter > 1) {
            sb.append("There were slightly better moves you could have made (such as " + new Move(((MoveWithEval)movesAvailableForChallenger.get(0)).uci, gameBoardClone).algebraicFormat() + ").");
        }
        else {
            sb.append("That was one of the best moves.");
        }

        try{
            LichessInterface.writeChat(game.getGameId(), sb.toString());
        }
        catch (LichessApiException e) {
            AppLogger.getInstance().error("Got LichessApiException while trying to write move evaluation to chat");
        }

        return true;
    }
}
