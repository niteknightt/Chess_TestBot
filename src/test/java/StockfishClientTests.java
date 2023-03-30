import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.common.Settings;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.PotentialMoves;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.testbot.moveselectors.InstructiveMoveSelector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

public class StockfishClientTests {
    @BeforeAll
    public static void beforeClass() {
        Settings.createInstance(Enums.SettingsType.BOTTERBOT);
        AppLogger.createInstance(Enums.SettingsType.BOTTERBOT, Enums.LogLevel.DEBUG, true);
    }
    @Test

    public boolean noMateInMoves(List<EvaluatedMove> moves, Enums.Color color) {
        for (EvaluatedMove move : moves) {
            if (move.ismate) {
                return false;
            }
        }
        return true;
    }

    public boolean evalsInOrder(List<EvaluatedMove> moves, Enums.Color color) {
        double prevEval = 0;
        boolean first = true;
        for (EvaluatedMove move : moves) {
            if (!first) {
                if (color == Enums.Color.WHITE) {
                    if (move.eval > prevEval) {
                        return false;
                    }
                }
                else {
                    if (move.eval < prevEval) {
                        return false;
                    }
                }
            }
            prevEval = move.eval;
        }
        return true;
    }

    @Test
    public void testAnotherBlackMoveWithWhiteMate() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("rr4k1/2RR1p2/6p1/pP2PpB1/nb6/5N2/5PPP/6K1 b - - 0 31");
        List<EvaluatedMove> moves = client.calcMoves(27, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.BLACK));
        Board board = new Board();
        board.setupFromFen("rr4k1/2RR1p2/6p1/pP2PpB1/nb6/5N2/5PPP/6K1 b - - 0 31");
        PotentialMoves potentialMoves = new PotentialMoves(moves, board);
        String str = potentialMoves.toString();
        Assertions.assertTrue(true);
    }

    @Test
    public void testAnotherBlackMoveWithBlackMate() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("rnb1kb1r/pp2pppp/1qp2n2/3p4/K2P4/8/PPP1PPPP/RNBQ1BNR b kq - 0 6");
        List<EvaluatedMove> moves = client.calcMoves(34, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.BLACK));
        Board board = new Board();
        board.setupFromFen("rnb1kb1r/pp2pppp/1qp2n2/3p4/K2P4/8/PPP1PPPP/RNBQ1BNR b kq - 0 6");
        PotentialMoves potentialMoves = new PotentialMoves(moves, board);
        String str = potentialMoves.toString();
        Assertions.assertTrue(true);
    }

    @Test
    public void testAnotherWhiteMoveWithBlackMate() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("rn1qkbnr/pp2pppp/4b3/3p4/1K1p4/8/PPP1PPPP/RNBQ1BNR w kq - 0 5");
        List<EvaluatedMove> moves = client.calcMoves(27, 2000, Enums.Color.WHITE);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
        Board board = new Board();
        board.setupFromFen("rn1qkbnr/pp2pppp/4b3/3p4/1K1p4/8/PPP1PPPP/RNBQ1BNR w kq - 0 5");
        PotentialMoves potentialMoves = new PotentialMoves(moves, board);
        String str = potentialMoves.toString();
        Assertions.assertTrue(true);
    }

    @Test
    public void testOpporunityForWhite() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("r1b2b1r/pp3P1p/3k4/n5N1/3p4/6P1/PP2PP1P/R3KB1R b KQ - 0 14");
        List<EvaluatedMove> moves = client.calcMoves(26, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.BLACK));
        Board board = new Board();
        board.setupFromFen("r1b2b1r/pp3P1p/3k4/n5N1/3p4/6P1/PP2PP1P/R3KB1R b KQ - 0 14");
        PotentialMoves potentialMoves = new PotentialMoves(moves, board);
        String str = potentialMoves.toString();
        GameLogger gameLogger = new GameLogger(Enums.LogLevel.DEBUG);

        InstructiveMoveSelector selector = new InstructiveMoveSelector(new Random(), Enums.EngineAlgorithm.INSTRUCTIVE, client, gameLogger, "dummy");
        int opportunityMoveIndex = selector.checkIfOpportunityExists(moves, potentialMoves, board);
        Assertions.assertTrue(true);
    }

    @Test
    public void testWhiteMovesWithoutMate() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        List<EvaluatedMove> moves = client.calcMoves(20, 2000, Enums.Color.WHITE);
        Assertions.assertTrue(noMateInMoves(moves, Enums.Color.WHITE));
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }

    @Test
    public void testBlackMovesWithoutMate() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1");
        List<EvaluatedMove> moves = client.calcMoves(20, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(noMateInMoves(moves, Enums.Color.BLACK));
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }

    @Test // TESTED
    public void testWhiteMovesWithMateForWhite() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("1k2B3/8/5r2/8/P1Q5/8/PP6/3R3K w - - 3 39");
        List<EvaluatedMove> moves = client.calcMoves(31, 2000, Enums.Color.WHITE);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }

    @Test // TESTED!!!
    public void testBlackMovesWithMateForBlack() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("8/p5p1/4R1k1/P1P3p1/1P6/2P1P3/r4r2/4K3 b - - 2 39");
        List<EvaluatedMove> moves = client.calcMoves(5, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }

    @Test // TESTED!!!
    public void testWhiteMovesWithMateForBlack() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("5r2/p5p1/6k1/2P1R1p1/PP6/2P1P3/r7/4K3 w - - 2 38");
        List<EvaluatedMove> moves = client.calcMoves(13, 2000, Enums.Color.WHITE);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }

    @Test
    public void testBlackMovesWithMateForWhite() {
        StockfishClient client = new StockfishClient();
        client.init(5000l, "1");
        client.startGame();
        client.setPosition("1k2B3/8/5r2/8/P1Q5/8/PP6/3R3K b - - 3 39");
        List<EvaluatedMove> moves = client.calcMoves(17, 2000, Enums.Color.BLACK);
        Assertions.assertTrue(evalsInOrder(moves, Enums.Color.WHITE));
    }
}