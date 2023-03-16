package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.Date;
import java.util.List;
import java.util.Random;

public class JustTheBestMoveSelector extends MoveSelector {

    public JustTheBestMoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        super(random, algorithm, stockfishClient, log, gameId);
    }

    public List<EvaluatedMove> getAllMoves(Board board) {
        throw new RuntimeException("getAllMoves not implemented for JustTheBestMoveSelector");
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
            boolean chooseRandomMove = false;
            _stockfishClient.setPosition(board.getFen());
            try {
                Date beforeCall = new Date();
                bestMoveUciFormat = _stockfishClient.calcBestMove(13, 5000);
                Date afterCall = new Date();
                long callTime = Math.abs(afterCall.getTime() - beforeCall.getTime());
                _log.info(_gameId, "moveselector", "justthebest;depth=13;moveNumber=" + board.getFullMoveNumber() + ";numPieces=" + board.getNumPiecesOnBoard() + ";numLegalMoves=" + legalMoves.size() + ";timeMs=" + callTime);
            }
            catch (Exception ex) {
                _log.error(_gameId, "moveselector", "Exception while calling calcBestMove: " + ex.toString() + " -- choosing random move");
                chooseRandomMove = true;
            }
            if (bestMoveUciFormat == null || bestMoveUciFormat.length() == 0) {
                _log.error(_gameId, "moveselector", "Failed to get best move from stockfish -- choosing random move");
                chooseRandomMove = true;
            }
            if (chooseRandomMove) {
                int index = _random.nextInt(legalMoves.size());
                bestMoveUciFormat = legalMoves.get(index).uciFormat();
                _log.debug(_gameId, "moveselector", "Best random move: " + bestMoveUciFormat);
            }
        }
        Move engineMove = new Move(bestMoveUciFormat, board);
        return engineMove;
    }

}
