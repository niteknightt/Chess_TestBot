package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class BestWorstMoveSelector extends MoveSelector {

    public BestWorstMoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        super(random, algorithm, stockfishClient, log, gameId);
    }

    public Move selectMove(Board board) throws MoveSelectorException {
        _log.debug(_gameId, "moveselector", "Starting BestWorst move selection with board having fen: " + board.getFen());
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
            _log.debug(_gameId, "moveselector", "Sending fen to stockfish: " + board.getFen());
            _stockfishClient.setPosition(board.getFen());
            List<EvaluatedMove> movesWithEval = new ArrayList<EvaluatedMove>();
            try {
                Date beforeCall = new Date();
                movesWithEval = _stockfishClient.calcMoves(legalMoves.size(), 2000, board.whosTurnToGo());
                Date afterCall = new Date();
                long callTime = Math.abs(afterCall.getTime() - beforeCall.getTime());
                _log.info(_gameId, "moveselector", "bestworst;depth=10;moveNumber=" + board.getFullMoveNumber() + ";numPieces=" + board.getNumPiecesOnBoard() + ";numLegalMoves=" + legalMoves.size() + ";timeMs=" + callTime);
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
                if (_algorithm == Enums.EngineAlgorithm.BEST_MOVE) {
                    bestMoveUciFormat = movesWithEval.get(0).uci;
                }
                else if (_algorithm == Enums.EngineAlgorithm.WORST_MOVE) {
                    bestMoveUciFormat = movesWithEval.get(movesWithEval.size()-1).uci;
                }
                else {
                    _log.error(_gameId, "moveselector", "Algorithm is not set correctly to play game -- value is " + _algorithm);
                    throw new MoveSelectorException();
                }
                _log.debug(_gameId, "moveselector", "Best move from multiPV stockfish: " + bestMoveUciFormat);
            }
        }
        Move engineMove = new Move(bestMoveUciFormat, board);
        return engineMove;
    }

}
