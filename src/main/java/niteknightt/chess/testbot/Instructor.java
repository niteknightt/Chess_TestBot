package niteknightt.chess.testbot;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Enums;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessInterface;
import niteknightt.chess.testbot.tests.PotentialMoves;

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

        StringBuilder sb = new StringBuilder();

        PotentialMoves humanPotentialMoves = game._humanPotentialMoves.get(game._numMovesPlayedByChallenger - 1);
        Move playedMove = game._challengerMoves.get(game._numMovesPlayedByChallenger - 1);

        if (playedMove.uciFormat().equals(humanPotentialMoves.evaluatedMoves.get(0).uci)) {
            sb.append("That was the best move!");
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
                if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.VERY_MUCH_BETTER_THAN_BEFORE) {
                    sb.append("Well played!");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.MUCH_BETTER_THAN_BEFORE) {
                    sb.append("Very good, but there were even better moves.");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                    sb.append("Not bad, but there were even better moves.");
                }
                else {
                    sb.append("You had much better options.");
                }
            }
            else if (humanPotentialMoves.numVeryMuchBetterMoves == 1) {
                if (humanPotentialMoves.numMuchBetterMoves > 0) {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.MUCH_BETTER_THAN_BEFORE) {
                        sb.append("Very good, but there was an even better move.");
                    }
                    else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                        sb.append("Not bad, but there were even better moves.");
                    }
                    else {
                        sb.append("You had much better options.");
                    }
                }
                else {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                        sb.append("Not bad, but there was an even better move.");
                    }
                    else {
                        sb.append("You had at least one much better option.");
                    }
                }
            }
            else if (humanPotentialMoves.numMuchBetterMoves >= 2) {
                if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.MUCH_BETTER_THAN_BEFORE) {
                    sb.append("Well played!");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                    sb.append("Nice, but there were even better moves.");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                    sb.append("You didn't lose ground, but there were better moves.");
                }
                else {
                    sb.append("You had much better options.");
                }
            }
            else if (humanPotentialMoves.numMuchBetterMoves == 1) {
                if (humanPotentialMoves.numSomewhatBetterMoves > 0) {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                        sb.append("Nice, but there was an even better move.");
                    }
                    else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                        sb.append("You didn't lose ground, but there were better moves.");
                    }
                    else {
                        sb.append("You had much better options.");
                    }
                }
                else {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                        sb.append("You didn't lose ground, but there was a better move.");
                    }
                    else {
                        sb.append("You had at least one much better option.");
                    }
                }
            }
            else if (humanPotentialMoves.numSomewhatBetterMoves >= 2) {
                if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_BETTER_THAN_BEFORE) {
                    sb.append("Well played!");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                    sb.append("That's OK, but there were better moves.");
                }
                else {
                    sb.append("You had much better options.");
                }
            }
            else if (humanPotentialMoves.numSomewhatBetterMoves == 1) {
                if (humanPotentialMoves.numSameMoves > 0) {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                        sb.append("That's OK, but there was a better move.");
                    }
                    else {
                        sb.append("You had much better options.");
                    }
                }
                else {
                    sb.append("You had at least one better option.");
                }
            }
            else if (humanPotentialMoves.numSameMoves >= 2) {
                if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SAME_AS_BEFORE) {
                    sb.append("You found one of the ways to hold your current eval.");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_WORSE_THAN_BEFORE) {
                    sb.append("You're losing some ground -- you had better options to hold your eval.");
                }
                else {
                    sb.append("You had much better options.");
                }
            }
            else if (humanPotentialMoves.numSameMoves == 1) {
                if (humanPotentialMoves.numSomewhatWorseMoves > 0) {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_WORSE_THAN_BEFORE) {
                        sb.append("You're losing some ground -- you had a better move to hold your eval.");
                    }
                    else {
                        sb.append("You had better options.");
                    }
                }
                else {
                    sb.append("You had at least one better option.");
                }
            }
            else if (humanPotentialMoves.numSomewhatWorseMoves >= 2) {
                if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.SOMEWHAT_WORSE_THAN_BEFORE) {
                    sb.append("You found one of the ways to slow the slide down.");
                }
                else if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.MUCH_WORSE_THAN_BEFORE) {
                    sb.append("You're quite worse now -- you had better options to slow the slide down.");
                }
                else {
                    sb.append("You had much better options.");
                }
            }
            else if (humanPotentialMoves.numSomewhatWorseMoves == 1) {
                if (humanPotentialMoves.numMuchWorseMoves > 0) {
                    if (playedEvaluatedMove.evalCategory == Enums.MoveEvalCategory.MUCH_WORSE_THAN_BEFORE) {
                        sb.append("You're quite worse now -- you had a better option to slow the slide down.");
                    }
                    else {
                        sb.append("You had better options.");
                    }
                }
                else {
                    sb.append("You had only one option to slow the slide down.");
                }
            }
            else {
                sb.append("Not much you could do in this situation -- all moves lead to a worse position.");
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
