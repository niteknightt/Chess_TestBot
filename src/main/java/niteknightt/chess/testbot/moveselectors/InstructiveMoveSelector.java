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

    protected boolean _isOpportunityForHuman = false;

    public boolean isOpportunityForHuman() { return _isOpportunityForHuman; }

    public InstructiveMoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        super(random, algorithm, stockfishClient, log, gameId);
    }

    public Move selectMove(Board board) throws MoveSelectorException {
        List<Move> legalMoves = board.getLegalMoves();
        _log.debug(_gameId, "moveselector", Move.printMovesToString("These are the legal moves", legalMoves));

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
                _log.info(_gameId, "moveselector", "instructive;depth=10;moveNumber=" + board.getFullMoveNumber() + ";numPieces=" + board.getNumPiecesOnBoard() + ";numLegalMoves=" + legalMoves.size() + ";timeMs=" + callTime);
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

                int opportunityMoveIndex = checkIfOpportunityExists(movesWithEval, board);

                if (opportunityMoveIndex != -1) {
                    bestMoveUciFormat = movesWithEval.get(opportunityMoveIndex).uci;
                    _isOpportunityForHuman = true;
                }
                else {
                    int finalIndex = -1;
                    PotentialMoves potentialMoves = new PotentialMoves(movesWithEval);
                    int numReasonableMoves = movesWithEval.size() - potentialMoves.numLosing - potentialMoves.numWellBehind;
                    if (numReasonableMoves == 0 || numReasonableMoves == 1) {
                        finalIndex = 0;
                    }
                    else {
                        Random random = new Random();
                        finalIndex = random.nextInt(numReasonableMoves);
                    }
                    bestMoveUciFormat = movesWithEval.get(finalIndex).uci;
                    _isOpportunityForHuman = false;
                    _log.debug(_gameId, "moveselector", "Selecting move index " + finalIndex + " out of " + movesWithEval.size() + " which is " +  movesWithEval.get(finalIndex).uci + " with eval category" + movesWithEval.get(finalIndex).evalCategory);
                }
            }
        }
        Move engineMove = new Move(bestMoveUciFormat, board);
        return engineMove;
    }

    protected int checkIfOpportunityExists(List<EvaluatedMove> movesWithEval, Board board) {
        int opportunityMoveIndex = -1;
        PotentialMoves potentialMoves = new PotentialMoves(movesWithEval);
        if (potentialMoves.numLosing > 0 || potentialMoves.numWellBehind > 0) {
            for (int moveIndex = movesWithEval.size() - 1; moveIndex >= movesWithEval.size() - potentialMoves.numLosing - potentialMoves.numWellBehind; --moveIndex) {
                EvaluatedMove currentEvaluatedMove = movesWithEval.get(moveIndex);
                Board afterMoveBoard = board.clone();
                afterMoveBoard.handleMoveForGame(new Move(currentEvaluatedMove.uci, afterMoveBoard));
                List<EvaluatedMove> humanPossibleMoves = _stockfishClient.calcMoves(board.getLegalMoves().size(), 2000, board.whosTurnToGo());
                PotentialMoves potentialHumanMoves = new PotentialMoves(humanPossibleMoves);
                if (humanPossibleMoves.size() > 1 &&
                        potentialHumanMoves.numWinning + potentialHumanMoves.numWellAhead == 1 &&
                        isNotACapture(humanPossibleMoves.get(0), afterMoveBoard) &&
                        isNotAMateIn(humanPossibleMoves.get(0))) {
                    opportunityMoveIndex = moveIndex;
                    _log.debug(_gameId, "moveselector", "Selecting opportunity move index " + moveIndex + " out of " + movesWithEval.size() + " which is " +  movesWithEval.get(moveIndex).uci + " with eval category" + movesWithEval.get(moveIndex).evalCategory + " because it gives the challenger an opportunity");
                    break;
                }
            }
        }

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
