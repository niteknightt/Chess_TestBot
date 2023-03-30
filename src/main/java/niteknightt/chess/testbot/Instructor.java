package niteknightt.chess.testbot;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Enums;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessInterface;

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
        //   * If engine has 2 or more moves which are VERY_WELL_AHEAD.
        //   * If engine has 1 move VERY_WELL_AHEAD and 1 or more moves
        //     that are WELL_AHEAD or LEADING.
        //   * If engine has 1 move VERY_WELL_AHEAD and all others
        //     are EQUAL or below.
        //   * If computer has 2 or more moves which are WELL_AHEAD.
        //   * if computer has 1 move WELL_AHEAD and 1 or more moves
        //     that are LEADING
        //   * If computer has 1 move WELL_AHEAD and all others
        //     are EQUAL or below.
        //   * If computer has 0 moves WELL_AHEAD, but 2 or more moves
        //     which are LEADING.
        //   * If computer has 0 moves WELL_AHEAD, but 1 move which is
        //     LEADING.
        //   * If computer has 0 moves WELL_AHEAD, and 0 moves which
        //     are LEADING.
        //
        //   * If human chose best move.
        //   * If human did not choose best move:
        //   *   If best move was in category VERY_MUCH_BETTER and there were more
        //       than one of these.
        //   *   If best move was in category VERY_MUCH_BETTER and there was only
        //       one of these but there was also at least one MUCH_BETTER or SOMEWHAT_BETTER.
        //   *   If best move was in category VERY_MUCH_BETTER and there was only
        //       one of these and there were no moves MUCH_BETTER or SOMEWHAT_BETTER.
        //   * If human did not choose best move, but chosen move is 1 category

        int numTotalMoves = game.moves().size();
        int moveNumber = numTotalMoves / 2;
        if (game.challengerColor() == Enums.Color.WHITE) {
            if ((numTotalMoves & 1) == 0) {
                throw new RuntimeException("Expected odd number of moves but got even");
            }
            ++moveNumber;
        }
        else if ((numTotalMoves & 1) != 0) {
            throw new RuntimeException("Expected even number of moves but got odd");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(moveNumber);
        sb.append(". ");
        sb.append(game.moves().get(game.moves().size() - 1).algebraicFormat());
        sb.append(": ");

        if (game._humanPotentialMoves.size() == 0) {
            return true;
        }
        if (game._numMovesPlayedByChallenger - 1 >= game._humanPotentialMoves.size()) {
            throw new RuntimeException("There have been " + game._numMovesPlayedByChallenger + " played by the challenger, but the number of saved potential move lists is only " + game._humanPotentialMoves.size());
        }

        PotentialMoves humanPotentialMoves = game._humanPotentialMoves.get(game._numMovesPlayedByChallenger - 1);
        Move playedMove = game._challengerMoves.get(game._numMovesPlayedByChallenger - 1);

        if (playedMove.uciFormat().equals(humanPotentialMoves.evaluatedMoves.get(0).uci)) {
            sb.append("That was the best move!");
            switch (humanPotentialMoves.evaluatedMoves.get(0).evalCategory) {
                case WINNING:
                    sb.append(" You now have a winning position.");
                    break;
                case WELL_AHEAD:
                    sb.append(" Your position is very much better than your opponent's.");
                    break;
                case LEADING:
                    sb.append(" Your position is now somewhat better than your opponent's.");
                    break;
                case EQUAL:
                    sb.append(" Your position remains about equal to your opponent's.");
                    break;
                case LAGGING:
                    sb.append(" But unfortunately, your position is somewhat worse than your opponent's.");
                    break;
                case WELL_BEHIND:
                    sb.append(" Alas, your position is much worse than your opponent's.");
                    break;
                case LOSING:
                    sb.append(" Either way, though, your are in a losing position.");
                    break;
                default:
                    throw new RuntimeException("The category for best move was not defined");
            }
        }
        else {
            EvaluatedMove playedEvaluatedMove = null;
            // Find played evaluated move
            for (EvaluatedMove evaluatedMove : humanPotentialMoves.evaluatedMoves) {
                if (playedMove.uciFormat().equals(evaluatedMove.uci)) {
                    playedEvaluatedMove = evaluatedMove;
                    break;
                }
            }

            if (playedEvaluatedMove == null) {
                throw new RuntimeException("Failed to find human played move in list of potential moves");
            }

            if (humanPotentialMoves.numWinning >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case WINNING:
                        sb.append("That was one of the best moves. Your position is winning.");
                        break;
                    case WELL_AHEAD:
                        sb.append("Your position is very good, but there were even better moves that would have given you a winning position.");
                        break;
                    case LEADING:
                        sb.append("Your position is better than your opponent's, but there were even better moves that would have given you a winning position.");
                        break;
                    case EQUAL:
                        sb.append("You missed moves that would have given you a winning position. Instead, you are about equal with your opponent.");
                        break;
                    case LAGGING:
                        sb.append("Your position is worse than your opponent's. You had moves that would have given you a winning position.");
                        break;
                    case WELL_BEHIND:
                        sb.append("Your position is very bad. You had moves that would have given you a winning position.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have given you a winning position.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numWinning == 1) {
                if (humanPotentialMoves.numWellAhead > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case WELL_AHEAD:
                            sb.append("Your position is very good, but there was an even better move that would have given you a winning position.");
                            break;
                        case LEADING:
                            sb.append("Your position is better than your opponent's, but there were better moves that would have given you a winning or very good position.");
                            break;
                        case EQUAL:
                            sb.append("You missed moves that would have given you a winning or very good position. Instead, you are about equal with your opponent.");
                            break;
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had moves that would have given you a winning or very good position.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had moves that would have given you a winning or very good position.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had moves that would have given you a winning or very good position.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LEADING:
                            sb.append("Your position is better than your opponent's, but there was an even better move that would have given you a winning position.");
                            break;
                        case EQUAL:
                            sb.append("You missed a move that would have given you a winning position. Instead, you are about equal with your opponent.");
                            break;
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had a move that would have given you a winning position.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had a move that would have given you a winning position.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have given you a winning position.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numWellAhead >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case WELL_AHEAD:
                        sb.append("That was one of the best moves. Your position is very good.");
                        break;
                    case LEADING:
                        sb.append("Your position is better than your opponent's, but there were even better moves that would have given you a very good position.");
                        break;
                    case EQUAL:
                        sb.append("You missed moves that would have given you a very good position. Instead, you are about equal with your opponent.");
                        break;
                    case LAGGING:
                        sb.append("Your position is worse than your opponent's. You had moves that would given you a very good position.");
                        break;
                    case WELL_BEHIND:
                        sb.append("Your position is very bad. You had moves that would given you a very good position.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have given you a very good position.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numWellAhead == 1) {
                if (humanPotentialMoves.numLeading > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LEADING:
                            sb.append("Your position is better than your opponent's, but there was an even better move that would have given you a very good position.");
                            break;
                        case EQUAL:
                            sb.append("You missed moves that would have given you a very good position or at least be better than your opponent. Instead, you are about equal with your opponent.");
                            break;
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had moves that would have given you a very good position or at least be better than your opponent.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had moves that would have given you a very good position or at least be better than your opponent.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had moves that would have given you a very good position or at least be better than your opponent.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case EQUAL:
                            sb.append("You missed moves that would have given you a very good position. Instead, you are about equal with your opponent.");
                            break;
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had a move that would have given you a very good position.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had a move that would have given you a very good position.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have given you a very good position.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numLeading >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case LEADING:
                        sb.append("That was one of the best moves. Your position is better than your opponent's.");
                        break;
                    case EQUAL:
                        sb.append("You missed moves that would have given you a better position than your opponent's. Instead, you are about equal with your opponent.");
                        break;
                    case LAGGING:
                        sb.append("Your position is worse than your opponent's. You had moves that would have given you a better position than your opponent's.");
                        break;
                    case WELL_BEHIND:
                        sb.append("Your position is very bad. You had moves that would have given you a better position than your opponent's.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have given you a better position than your opponent's.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numLeading == 1) {
                if (humanPotentialMoves.numEqual > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case EQUAL:
                            sb.append("Your position is about equal with your opponent, but there was a better move that would have given you a better position than your opponent's.");
                            break;
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had moves that would have given you a better position than your opponent's or at least made it about equal with your opponent.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had moves that would have given you a better position than your opponent's or at least made it about equal with your opponent.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had moves that would have given you a better position than your opponent's or at least made it about equal with your opponent.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. You had a move that would have given you a better position than your opponent's.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had a move that would have given you a better position than your opponent's.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have given you a better position than your opponent's.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numEqual >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case EQUAL:
                        sb.append("That was one of the best moves. Now your position is about equal with your opponent.");
                        break;
                    case LAGGING:
                        sb.append("Your position is worse than your opponent's. You had moves that would have made it about equal with your opponent.");
                        break;
                    case WELL_BEHIND:
                        sb.append("Your position is very bad. You had moves that would have made it about equal with your opponent.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have made it about equal with your opponent.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numEqual == 1) {
                if (humanPotentialMoves.numLagging > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LAGGING:
                            sb.append("Your position is worse than your opponent's. There was a move that would have made it about equal with your opponent.");
                            break;
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had moves that would have made it about equal with your opponent or at worst made it just worse than your opponent's.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had moves that would have made it about equal with your opponent or at worst made it just worse than your opponent's.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. You had a move that would have made it about equal with your opponent.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have made it about equal with your opponent.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numLagging >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case LAGGING:
                        sb.append("That was one of the best moves. Even so, your position is worse than your opponent's.");
                        break;
                    case WELL_BEHIND:
                        sb.append("Your position is very bad. You had moves that would have only made it just worse than your opponent's.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have only made it just worse than your opponent's.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numLagging == 1) {
                if (humanPotentialMoves.numWellBehind > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case WELL_BEHIND:
                            sb.append("Your position is very bad. There was a move that would have only made it just worse than your opponent's.");
                            break;
                        case LOSING:
                            sb.append("Your position is losing. You had moves that would have only made it just worse than your opponent's or made it very bad but not losing.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have have only made it just worse than your opponent's.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numWellBehind >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case WELL_BEHIND:
                        sb.append("That was one of the best moves. But it doesn't help much -- your position is very bad.");
                        break;
                    case LOSING:
                        sb.append("Your position is losing. You had moves that would have made it very bad but not losing.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numWellBehind == 1) {
                if (humanPotentialMoves.numLosing > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case LOSING:
                            sb.append("Your position is losing. You had a move that would have made it very bad but not losing.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    throw new RuntimeException("This case should not happen");
                }
            }
            else if (humanPotentialMoves.numLosing >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case LOSING:
                        sb.append("Your position is losing -- you had no good moves.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numWellBehind == 1) {
                throw new RuntimeException("This case should not happen");
            }
            else {
                throw new RuntimeException("This case should not happen");
            }
        }

        try{
            LichessInterface.writeChat(game.getGameId(), sb.toString());
        }
        catch (LichessApiException e) {
            AppLogger.getInstance().error("Got LichessApiException while trying to write move evaluation to chat");
        }

        return true;
    }

    public static boolean reviewLastHumanMove1(BotGameVsHuman game) {
        List<EvaluatedMove> movesAvailableForChallenger = null;

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
            EvaluatedMove availableMove = movesAvailableForChallenger.get(i);
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

        EvaluatedMove challengerEvaluatedMove = movesAvailableForChallenger.get(challengerMoveIndex);
        boolean bestMove = false;

        if (challengerMoveIndex == 0) {
            bestMove = true;
        }
        else {
            numMovesBetter = challengerMoveIndex;
            for (int i = 0; i < numMovesBetter; ++i) {
                EvaluatedMove availableMove = movesAvailableForChallenger.get(i);
                double evalDiff = Math.abs(availableMove.eval - challengerEvaluatedMove.eval);
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
            if (challengerEvaluatedMove.continuation.length > 0) {
                Move move = new Move(challengerEvaluatedMove.uci, gameBoardClone);
                gameBoardClone.handleMoveForGame(move);
                sb.append(" I might now play " + new Move(challengerEvaluatedMove.continuation[0], gameBoardClone).algebraicFormat());
                gameBoardClone.undoSingleAnalysisMove(move);
            }
        }
        else if (numMovesMuchBetter > 1) {
            sb.append("There were much better moves you could have made (such as " + new Move(movesAvailableForChallenger.get(0).uci, gameBoardClone).algebraicFormat() + ").");
            if (challengerEvaluatedMove.continuation.length > 0) {
                Move move = new Move(challengerEvaluatedMove.uci, gameBoardClone);
                gameBoardClone.handleMoveForGame(move);
                sb.append(" I might now play " + new Move(challengerEvaluatedMove.continuation[0], gameBoardClone).algebraicFormat());
                gameBoardClone.undoSingleAnalysisMove(move);
            }
        }
        else  if (numMovesALittleBetter == 1) {
            sb.append("There was a slightly better move you could have made (" + new Move(movesAvailableForChallenger.get(0).uci, gameBoardClone).algebraicFormat() + ").");
        }
        else if (numMovesALittleBetter > 1) {
            sb.append("There were slightly better moves you could have made (such as " + new Move(((EvaluatedMove)movesAvailableForChallenger.get(0)).uci, gameBoardClone).algebraicFormat() + ").");
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
