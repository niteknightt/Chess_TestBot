package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.List;
import java.util.Random;

public abstract class MoveSelector {
    protected Random _random;
    protected StockfishClient _stockfishClient;
    protected Enums.EngineAlgorithm _algorithm;
    protected GameLogger _log;
    protected String _gameId;

    public MoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        _random = random;
        _stockfishClient = stockfishClient;
        _algorithm = algorithm;
        _log = log;
        _gameId = gameId;
    }

    public abstract List<EvaluatedMove> getAllMoves(Board board);

    public abstract Move selectMove(Board board)
            throws MoveSelectorException;
}
