package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.*;

public class InstructiveMoveSelector extends MoveSelector {

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
                _log.error(_gameId, "moveselector", "Exception while calling calcMoves: " + ex.toString());
            }
            if (movesWithEval.size() == 0) {
                _log.error(_gameId, "moveselector", "Zero moves from stockfish even though there are legal moves");
                int index = _random.nextInt(legalMoves.size());
                bestMoveUciFormat = legalMoves.get(index).uciFormat();
                _log.debug(_gameId, "moveselector", "Best random move: " + bestMoveUciFormat);
            }
            else {
                if (movesWithEval.size() != legalMoves.size()) {
                    _log.error(_gameId, "moveselector", "Number of moves from stockfish (" + movesWithEval.size() + ") is not the same as number of legal moves (" + legalMoves.size() + ")");
                }
                int closestIndex = -1;
                double diff = 1000.0;
                for (int i = 0; i < movesWithEval.size(); ++i) {
                    if (Math.abs(movesWithEval.get(i).eval) < diff) {
                        diff = Math.abs(movesWithEval.get(i).eval);
                        closestIndex = i;
                    }
                }
                bestMoveUciFormat = movesWithEval.get(closestIndex).uci;
                _log.debug(_gameId, "moveselector", "Best move to keep close to eval zero: " + bestMoveUciFormat);
            }
        }
        Move engineMove = new Move(bestMoveUciFormat, board);
        return engineMove;
    }

    public List<EvaluatedMove> getAllMoves(Board board) {
        List<Move> legalMoves = board.getLegalMoves();
        _log.debug(_gameId, "moveselector", Move.printMovesToString("These are the legal moves", legalMoves));

        String bestMoveUciFormat = "";

        if (legalMoves.size() == 0) {
            return null;
        }

        if (legalMoves.size() == 1) {
            bestMoveUciFormat =  legalMoves.get(0).uciFormat();
            EvaluatedMove evaluatedMove = new EvaluatedMove();
            evaluatedMove.eval = -1000.0;
            evaluatedMove.evalCategory = Enums.MoveEvalCategory.SAME_AS_BEFORE;
            evaluatedMove.ismate = false;
            evaluatedMove.matein = 0;
            evaluatedMove.uci = bestMoveUciFormat;
            return Arrays.asList(evaluatedMove);
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
                _log.error(_gameId, "moveselector", "Exception while calling calcMoves: " + ex.toString());
            }
            if (movesWithEval.size() == 0) {
                _log.error(_gameId, "moveselector", "Zero moves from stockfish even though there are legal moves");
                int index = _random.nextInt(legalMoves.size());
                bestMoveUciFormat = legalMoves.get(index).uciFormat();
                EvaluatedMove evaluatedMove = new EvaluatedMove();
                evaluatedMove.eval = -1000.0;
                evaluatedMove.evalCategory = Enums.MoveEvalCategory.SAME_AS_BEFORE;
                evaluatedMove.ismate = false;
                evaluatedMove.matein = 0;
                evaluatedMove.uci = bestMoveUciFormat;
                return Arrays.asList(evaluatedMove);
            }
            else {
                if (movesWithEval.size() != legalMoves.size()) {
                    _log.error(_gameId, "moveselector", "Number of moves from stockfish (" + movesWithEval.size() + ") is not the same as number of legal moves (" + legalMoves.size() + ")");
                }
                return movesWithEval;
            }
        }
    }

}
