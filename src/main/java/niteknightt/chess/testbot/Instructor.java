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
        //   * If engine has 2 or more moves which are VERY_MUCH_BETTER_THAN_BEFORE.
        //   * If engine has 1 move VERY_MUCH_BETTER_THAN_BEFORE and 1 or more moves
        //     that are MUCH_BETTER_THAN_BEFORE or SOMEWHAT_BETTER_THAN_BEFORE.
        //   * If engine has 1 move VERY_MUCH_BETTER_THAN_BEFORE and all others
        //     are SAME_AS_BEFORE or below.
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
                case VERY_MUCH_BETTER_THAN_BEFORE:
                    sb.append(" Your position is now very much better than before.");
                    break;
                case MUCH_BETTER_THAN_BEFORE:
                    sb.append(" Your position is very much better than before.");
                    break;
                case SOMEWHAT_BETTER_THAN_BEFORE:
                    sb.append(" Your position is now somewhat better than before.");
                    break;
                case SAME_AS_BEFORE:
                    sb.append(" Your position remains about the same as before.");
                    break;
                case SOMEWHAT_WORSE_THAN_BEFORE:
                    sb.append(" But unfortunately, your position is somewhat worse than before.");
                    break;
                case MUCH_WORSE_THAN_BEFORE:
                    sb.append(" Alas, your position is much worse than before.");
                    break;
                case VERY_MUCH_WORSE_THAN_BEFORE:
                    sb.append(" Either way, though, your position is very much worse than before.");
                    break;
                default:
                    throw new RuntimeException("The category for best move was incorrect");
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

            if (humanPotentialMoves.numVeryMuchBetterMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case VERY_MUCH_BETTER_THAN_BEFORE:
                        sb.append("That was one of the best moves. Now your position is very much better than before.");
                        break;
                    case MUCH_BETTER_THAN_BEFORE:
                        sb.append("You much improved your position from before, but there were even better moves that would have greatly improved it.");
                        break;
                    case SOMEWHAT_BETTER_THAN_BEFORE:
                        sb.append("You improved your position from before, but there were even better moves that would have greatly improved it.");
                        break;
                    case SAME_AS_BEFORE:
                        sb.append("You missed moves that would have greatly improved your position. Instead, it remains about the same.");
                        break;
                    case SOMEWHAT_WORSE_THAN_BEFORE:
                        sb.append("Your position has degraded somewhat. You had moves that would have greatly improved it.");
                        break;
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position has become much worse. You had moves that would have greatly improved it.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had moves that would have greatly improved it.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numVeryMuchBetterMoves == 1) {
                if (humanPotentialMoves.numMuchBetterMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case MUCH_BETTER_THAN_BEFORE:
                            sb.append("You much improved your position from before, but there was an even better move that would have greatly improved it.");
                            break;
                        case SOMEWHAT_BETTER_THAN_BEFORE:
                            sb.append("You improved your position from before, but there were even better moves that would have greatly or much improved it.");
                            break;
                        case SAME_AS_BEFORE:
                            sb.append("You missed moves that would have greatly or much improved your position. Instead, it remains about the same.");
                            break;
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat. You had moves that would have greatly or much improved it.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had moves that would have greatly or much improved it.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had moves that would have greatly or much improved it.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SOMEWHAT_BETTER_THAN_BEFORE:
                            sb.append("You improved your position from before, but there was an even better move that would have greatly improved it.");
                            break;
                        case SAME_AS_BEFORE:
                            sb.append("You missed a move that would have greatly improved your position. Instead, it remains about the same.");
                            break;
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat You had a move that would have greatly improved it.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse You had a move that would have greatly improved it.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse You had a move that would have greatly improved it.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numMuchBetterMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case MUCH_BETTER_THAN_BEFORE:
                        sb.append("That was one of the best moves. Now your position is much better than before.");
                        break;
                    case SOMEWHAT_BETTER_THAN_BEFORE:
                        sb.append("You improved your position from before, but there were even better moves that would have much improved it.");
                        break;
                    case SAME_AS_BEFORE:
                        sb.append("You missed moves that would have much improved your position. Instead, it remains about the same.");
                        break;
                    case SOMEWHAT_WORSE_THAN_BEFORE:
                        sb.append("Your position has degraded somewhat. You had moves that would have much improved it.");
                        break;
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position has become much worse. You had moves that would have much improved it.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse You had moves that would have much improved it.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numMuchBetterMoves == 1) {
                if (humanPotentialMoves.numSomewhatBetterMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SOMEWHAT_BETTER_THAN_BEFORE:
                            sb.append("You improved your position from before, but there was an even better move that would have much improved it.");
                            break;
                        case SAME_AS_BEFORE:
                            sb.append("You missed moves that would have improved or much improved your position. Instead, it remains about the same.");
                            break;
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat You had moves that would have improved or much improved it.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had moves that would have improved or much improved it.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had moves that would have improved or much improved it.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SAME_AS_BEFORE:
                            sb.append("You missed a move that would have much improved your position. Instead, it remains about the same.");
                            break;
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat You had a move that would have much improved it.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had a move that would have much improved it.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had a move that would have much improved it.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numSomewhatBetterMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case SOMEWHAT_BETTER_THAN_BEFORE:
                        sb.append("That was one of the best moves. Now your position is better than before.");
                        break;
                    case SAME_AS_BEFORE:
                        sb.append("You missed moves that would have improved your position. Instead, it remains about the same.");
                        break;
                    case SOMEWHAT_WORSE_THAN_BEFORE:
                        sb.append("Your position has degraded somewhat. You had moves that would have improved it.");
                        break;
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position has become much worse. You had moves that would have improved it.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had moves that would have improved it.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numSomewhatBetterMoves == 1) {
                if (humanPotentialMoves.numSameMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SAME_AS_BEFORE:
                            sb.append("You kept your position about the same, but there was a better move that would have improved it.");
                            break;
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat. You had moves that would have improved it or kept it the same.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had moves that would have improved it or kept it the same.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had moves that would have improved it or kept it the same.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat. You had a move that would have improved it.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had a move that would have improved it.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had a move that would have improved it.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numSameMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case SAME_AS_BEFORE:
                        sb.append("That was one of the best moves. Now your position is about the same as before.");
                        break;
                    case SOMEWHAT_WORSE_THAN_BEFORE:
                        sb.append("Your position has degraded somewhat. You had moves that would have kept it about the same as before.");
                        break;
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position has become much worse. You had moves that would have kept it about the same as before.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had moves that would have kept it about the same as before.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numSameMoves == 1) {
                if (humanPotentialMoves.numSomewhatWorseMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case SOMEWHAT_WORSE_THAN_BEFORE:
                            sb.append("Your position has degraded somewhat. There was a move that would have kept it the same as before.");
                            break;
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had moves that would have kept it the same as before or only degraded it somewhat.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had moves that would have kept it the same as before or only degraded it somewhat.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. You had a move that would have kept it the same as before.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had a move that would have kept it the same as before.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numSomewhatWorseMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case SOMEWHAT_WORSE_THAN_BEFORE:
                        sb.append("That was one of the best moves. Even so, now your position has degraded somewhat.");
                        break;
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position has become much worse. You had moves that would have only degraded it somewhat.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had moves that would have only degraded it somewhat.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numSomewhatWorseMoves == 1) {
                if (humanPotentialMoves.numMuchWorseMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position has become much worse. There was a move that would have only degraded it somewhat.");
                            break;
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had moves that would have only degraded it somewhat or made it only much worse instead of very much worse.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    switch (playedEvaluatedMove.evalCategory) {
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had a move that would have have only degraded it somewhat.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
            }
            else if (humanPotentialMoves.numMuchWorseMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case MUCH_WORSE_THAN_BEFORE:
                        sb.append("That was one of the best moves. But it doesn't help much -- your position has become much worse.");
                        break;
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had moves that would have made it only much worse instead of very much worse.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numMuchWorseMoves == 1) {
                if (humanPotentialMoves.numVeryMuchWorseMoves > 0) {
                    switch (playedEvaluatedMove.evalCategory) {
                        case VERY_MUCH_WORSE_THAN_BEFORE:
                            sb.append("Your position is now very much worse. You had a move that would have made it only much worse instead of very much worse.");
                            break;
                        default:
                            throw new RuntimeException("Failed to get an evaluation category for the human's move");
                    }
                }
                else {
                    throw new RuntimeException("This case should not happen");
                }
            }
            else if (humanPotentialMoves.numVeryMuchWorseMoves >= 2) {
                switch (playedEvaluatedMove.evalCategory) {
                    case VERY_MUCH_WORSE_THAN_BEFORE:
                        sb.append("Your position is now very much worse. You had no other choices.");
                        break;
                    default:
                        throw new RuntimeException("Failed to get an evaluation category for the human's move");
                }
            }
            else if (humanPotentialMoves.numMuchWorseMoves == 1) {
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
