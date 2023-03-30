package niteknightt.chess.testbot;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Enums;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessInterface;

public class Instructor {

    public static String getMoveReview(BotGameVsHuman game) {

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
            throw new RuntimeException("Tried to get move review when there were no moves to play");
        }
        if (game._numMovesPlayedByChallenger - 1 >= game._humanPotentialMoves.size()) {
            throw new RuntimeException("There have been " + game._numMovesPlayedByChallenger + " played by the challenger, but the number of saved potential move lists is only " + game._humanPotentialMoves.size());
        }

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

            switch (playedEvaluatedMove.evalCategory) {
                case WINNING:
                    sb.append("Great move - one of the best available.");
                    break;
                case WELL_AHEAD:
                    if (humanPotentialMoves.numWinning == 0) {
                        sb.append("Excellent move - one of the best available.");
                    } else {
                        if (humanPotentialMoves.numWinning > 1) {
                            sb.append("Excellent move - but there were better.");
                        } else {
                            sb.append("Excellent move - but there was a better one.");
                        }
                    }
                    break;
                case LEADING:
                    if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead == 0) {
                        sb.append("Good move - one of the best you had available.");
                    } else {
                        if (humanPotentialMoves.numWinning > 1) {
                            sb.append("Would be a good move - but there were much better.");
                        } else if (humanPotentialMoves.numWinning > 0) {
                            sb.append("Would be a good move - but there was a much better one.");
                        } else if (humanPotentialMoves.numWellAhead > 1) {
                            sb.append("Good move - but there were better.");
                        } else {
                            sb.append("Good move - but there was a better one.");
                        }
                    }
                    break;
                case EQUAL:
                    if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading == 0) {
                        sb.append("Good move - one of the best you had available.");
                    } else {
                        if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead > 1) {
                            sb.append("Would be an OK move - but there were much better.");
                        } else if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead > 0) {
                            sb.append("Would be an OK move - but there was a much better one.");
                        } else if (humanPotentialMoves.numLeading > 1) {
                            sb.append("An OK move - but there were better.");
                        } else {
                            sb.append("An OK move - but there was a better one.");
                        }
                    }
                    break;
                case LAGGING:
                    if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual == 0) {
                        sb.append("Good move - one of the best you had available.");
                    } else {
                        if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading > 1) {
                            sb.append("Would be an OK move - but there were much better.");
                        } else if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading > 0) {
                            sb.append("Would be an OK move - but there was a much better one.");
                        } else if (humanPotentialMoves.numEqual > 1) {
                            sb.append("An OK move - but there were better.");
                        } else {
                            sb.append("An OK move - but there was a better one.");
                        }
                    }
                    break;
                case WELL_BEHIND:
                    if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual + humanPotentialMoves.numLagging == 0) {
                        sb.append("Good move - one of the best you had available.");
                    } else {
                        if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual > 1) {
                            sb.append("Would be an OK move - but there were much better.");
                        } else if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual > 0) {
                            sb.append("Would be an OK move - but there was a much better one.");
                        } else if (humanPotentialMoves.numLagging > 1) {
                            sb.append("An OK move - but there were better.");
                        } else {
                            sb.append("An OK move - but there was a better one.");
                        }
                    }
                    break;
                case LOSING:
                    if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual + humanPotentialMoves.numLagging + humanPotentialMoves.numWellBehind == 0) {
                        sb.append("Good move - one of the best you had available.");
                    } else {
                        if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual + humanPotentialMoves.numLagging > 1) {
                            sb.append("Would be an OK move - but there were much better.");
                        } else if (humanPotentialMoves.numWinning + humanPotentialMoves.numWellAhead + humanPotentialMoves.numLeading + humanPotentialMoves.numEqual + humanPotentialMoves.numLagging > 0) {
                            sb.append("Would be an OK move - but there was a much better one.");
                        } else if (humanPotentialMoves.numWellBehind > 1) {
                            sb.append("An OK move - but there were better.");
                        } else {
                            sb.append("An OK move - but there was a better one.");
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Failed to get an evaluation category for the human's move");
            }
        }

        game._gameLogger.debug(game.getGameId(), "game", "Move review: " + sb);

        return sb.toString();
    }

    public static String getPositionEval(BotGameVsHuman game) {

        StringBuilder sb = new StringBuilder();

        if (game._humanPotentialMoves.size() == 0) {
            throw new RuntimeException("Tried to get move review when there were no moves to play");
        }
        if (game._numMovesPlayedByChallenger - 1 >= game._humanPotentialMoves.size()) {
            throw new RuntimeException("There have been " + game._numMovesPlayedByChallenger + " played by the challenger, but the number of saved potential move lists is only " + game._humanPotentialMoves.size());
        }

        PotentialMoves humanPotentialMoves = game._humanPotentialMoves.get(game._numMovesPlayedByChallenger - 1);
        Move playedMove = game._challengerMoves.get(game._numMovesPlayedByChallenger - 1);

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

        switch (playedEvaluatedMove.evalCategory) {
            case WINNING:
                sb.append("Your position is winning.");
                break;
            case WELL_AHEAD:
                sb.append("Your position is much better than mine.");
                break;
            case LEADING:
                sb.append("Your position is better than mine.");
                break;
            case EQUAL:
                sb.append("Your position is about equal to mine.");
                break;
            case LAGGING:
                sb.append("Your position is worse than mine.");
                break;
            case WELL_BEHIND:
                sb.append("Your position is much worse than mine.");
                break;
            case LOSING:
                sb.append("Your position is losing.");
                break;
            default:
                throw new RuntimeException("Failed to get an evaluation category for the human's move");
        }

        game._gameLogger.debug(game.getGameId(), "game", "Position eval: " + sb);

        return sb.toString();
    }

    public static boolean reviewLastHumanMove(BotGameVsHuman game) {

        String moveReviewText = getMoveReview(game);
        String positionEvalText = getPositionEval(game);

        try{
            LichessInterface.writeChat(game.getGameId(), moveReviewText);
        }
        catch (LichessApiException e) {
            AppLogger.getInstance().error("Got LichessApiException while trying to write move review to chat");
        }

        try{
            LichessInterface.writeChat(game.getGameId(), positionEvalText);
        }
        catch (LichessApiException e) {
            AppLogger.getInstance().error("Got LichessApiException while trying to write position eval to chat");
        }

        return true;
    }

}
