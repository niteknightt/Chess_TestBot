package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.PotentialMoves;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.*;

public class InstructiveMoveSelector extends MoveSelector {

    /**
     * The number of moves in a game that is considered the "start" of the game.
     * During these moves, the engine will play more reasonably than later in the game.
     */
    public static int NUM_MOVES_CONSIDERED_START = 8;

    /**
     * The number of top moves that the engine will consider during the "start" of the game.
     */
    public static int MAX_REASONABLE_MOVES_AT_START = 3;

    protected boolean _isOpportunityForHuman = false;
    protected Random _random = new Random();

    public boolean isOpportunityForHuman() { return _isOpportunityForHuman; }

    public InstructiveMoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        super(random, algorithm, stockfishClient, log, gameId);
    }

    public Move selectMove(Board board) throws MoveSelectorException {
        List<Move> legalMoves = board.getLegalMoves();
        //_log.debug(_gameId, "moveselector", Move.printMovesToString("These are the legal moves", legalMoves));

        String bestMoveUciFormat = "";

        if (legalMoves.size() == 0) {
            _log.info(_gameId, "moveselector", "Ending game internally because no legal moves for engine");
            return null;
        }

        if (legalMoves.size() == 1) {
            bestMoveUciFormat =  legalMoves.get(0).uciFormat();
            _log.debug(_gameId, "moveselector", "Best move for lack of choice: " + bestMoveUciFormat);
        }
        else {
            _stockfishClient.setPosition(board.getFen());
            List<EvaluatedMove> movesWithEval = new ArrayList<EvaluatedMove>();
            try {
                Date beforeCall = new Date();
                movesWithEval = _stockfishClient.calcMoves(board.getLegalMoves().size(), 2000, board.whosTurnToGo());
                Date afterCall = new Date();
                long callTime = Math.abs(afterCall.getTime() - beforeCall.getTime());
                //_log.info(_gameId, "moveselector", "instructive;depth=10;moveNumber=" + board.getFullMoveNumber() + ";numPieces=" + board.getNumPiecesOnBoard() + ";numLegalMoves=" + legalMoves.size() + ";timeMs=" + callTime);
            }
            catch (Exception ex) {
                throw new RuntimeException("Exception while calling calcMoves: " + ex);
            }
            if (movesWithEval.size() == 0) {
                throw new RuntimeException("Zero moves from stockfish even though there are legal moves");
            }
            else {
                if (movesWithEval.size() != legalMoves.size()) {
                    throw new RuntimeException("Number of moves from stockfish (" + movesWithEval.size() + ") is not the same as number of legal moves (" + legalMoves.size() + ")");
                }

                PotentialMoves potentialMoves = new PotentialMoves(movesWithEval, board);
                _log.debug(_gameId, "moveselector", "Potential moves for engine: " + potentialMoves);

                int opportunityMoveIndex = checkIfOpportunityExists(movesWithEval, potentialMoves, board);

                if (opportunityMoveIndex != -1) {
                    bestMoveUciFormat = movesWithEval.get(opportunityMoveIndex).uci;
                    _isOpportunityForHuman = true;
                }
                else {
                    int finalIndex = -1;
                    if (potentialMoves.numWinning > 0) {
                        finalIndex = _random.nextInt(potentialMoves.numWinning);
                        _log.debug(_gameId, "moveselector", "There are " + potentialMoves.numWinning + " winning moves - selected index " + finalIndex + " move " + new Move(movesWithEval.get(finalIndex).uci, board).algebraicFormat());
                    }
                    else if (potentialMoves.numWellAhead > 0) {
                        finalIndex = _random.nextInt(potentialMoves.numWellAhead + potentialMoves.numLeading);
                        _log.debug(_gameId, "moveselector", "There are " + potentialMoves.numWellAhead + " well-ahead moves and " + potentialMoves.numLeading + " leading moves - selected index " + finalIndex + " move " + new Move(movesWithEval.get(finalIndex).uci, board).algebraicFormat());
                    }
                    else {
                        int numReasonableMoves = movesWithEval.size() - potentialMoves.numLosing - potentialMoves.numWellBehind;
                        if (board.getFullMoveNumber() <= NUM_MOVES_CONSIDERED_START) {
                            numReasonableMoves = Math.min(numReasonableMoves - potentialMoves.numLagging, MAX_REASONABLE_MOVES_AT_START);
                        }
                        if (numReasonableMoves == 0 || numReasonableMoves == 1) {
                            finalIndex = 0;
                        }
                        else {
                            finalIndex = _random.nextInt(numReasonableMoves);
                        }
                        _log.debug(_gameId, "moveselector", "There are " + numReasonableMoves + " reasonable moves - selected index " + finalIndex + " move " + new Move(movesWithEval.get(finalIndex).uci, board).algebraicFormat());
                    }
                    bestMoveUciFormat = movesWithEval.get(finalIndex).uci;
                    _isOpportunityForHuman = false;
                }
            }
        }
        Move engineMove = new Move(bestMoveUciFormat, board);
        return engineMove;
    }

    public int checkIfOpportunityExists(List<EvaluatedMove> movesWithEval, PotentialMoves potentialMoves, Board board) {
        if (potentialMoves.numWinning + potentialMoves.numWellAhead + potentialMoves.numLeading + potentialMoves.numLagging == 0) {
            // The possible engine moves are bad, so everything is an opportunity for the
            // challenger, so basically nothing is.
            return -1;
        }

        int opportunityMoveIndex = -1;
        if (potentialMoves.numLosing > 0 || potentialMoves.numWellBehind > 0) {
            for (int moveIndex = movesWithEval.size() - 1; moveIndex >= movesWithEval.size() - potentialMoves.numLosing - potentialMoves.numWellBehind; --moveIndex) {
                EvaluatedMove currentEvaluatedMove = movesWithEval.get(moveIndex);
                Board afterMoveBoard = board.clone();
                boolean result = afterMoveBoard.handleMoveForGame(new Move(currentEvaluatedMove.uci, afterMoveBoard));
                _stockfishClient.setPosition(afterMoveBoard.getFen());
                List<EvaluatedMove> humanPossibleMoves = _stockfishClient.calcMoves(afterMoveBoard.getLegalMoves().size(), 2000, afterMoveBoard.whosTurnToGo());
                PotentialMoves potentialHumanMoves = new PotentialMoves(humanPossibleMoves, afterMoveBoard);
                if (humanPossibleMoves.size() > 1 &&
                        potentialHumanMoves.numWinning + potentialHumanMoves.numWellAhead == 1 &&
                        isNotACapture(humanPossibleMoves.get(0), afterMoveBoard) &&
                        isNotAMateIn(humanPossibleMoves.get(0))) {
                    opportunityMoveIndex = moveIndex;
                    _log.debug(_gameId, "moveselector", "Selecting opportunity move index " + moveIndex + " move " + movesWithEval.get(moveIndex).uci + " because challenger can play " + humanPossibleMoves.get(0).uci + " which evals to " + humanPossibleMoves.get(0).eval + " " + humanPossibleMoves.get(0).evalCategory);
                    break;
                }
            }
        }

        _stockfishClient.setPosition(board.getFen());

        return opportunityMoveIndex;
    }

    public static boolean isNotACapture(EvaluatedMove evaluatedMove, Board board) {
        Move move = new Move(evaluatedMove.uci, board);
        return !move.isCapture();
    }

    public static boolean isNotAMateIn(EvaluatedMove evaluatedMove) {
        return !evaluatedMove.ismate;
    }

}
