package niteknightt.chess.testbot.tests;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.testbot.moveselectors.BestWorstMoveSelector;
import niteknightt.chess.testbot.moveselectors.JustTheBestMoveSelector;
import niteknightt.chess.testbot.moveselectors.MoveSelector;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.gameplay.NotationConverter;
import niteknightt.chess.lichessapi.LichessChatItem;
import niteknightt.chess.lichessapi.LichessInterface;

import java.io.File;
import java.util.List;
import java.util.Random;

public class MainTests {
/*
    @BeforeEach
    public void setup() {
        File file = new File("resources");
        if (!file.exists()) {
            new File("resources").mkdirs();
        }
    }

    @Test
    void createBoard() {
        Board board = new Board();
    }

    @Test
    void setupBoardStartingPosition() {
        Board board = new Board();
        board.setupStartingPosition();
    }

    @Test
    void setupBoardMiddleGamePosition() {
        Board board = new Board();
        board.setupFromFen("r1b2rk1/ppp2ppp/1bnq4/3P4/2B5/2P2N2/P4PPP/R1BQ1RK1 b - - 0 12");
    }

    @Test
    void playGame(){
        Board board = new Board();
        board.setupStartingPosition();
        assertTrue(board.handleMoveForGame(new Move("d2d4", board)));
        assertTrue(board.handleMoveForGame(new Move("g8f6", board)));
        assertTrue(board.handleMoveForGame(new Move("c2c4", board)));
        assertTrue(board.handleMoveForGame(new Move("e7e6", board)));
        assertTrue(board.handleMoveForGame(new Move("g2g3", board)));
        assertTrue(board.handleMoveForGame(new Move("d7d5", board)));
        assertTrue(board.handleMoveForGame(new Move("f1g2", board)));
        assertTrue(board.handleMoveForGame(new Move("d5c4", board)));
        assertTrue(board.handleMoveForGame(new Move("g1f3", board)));
        assertTrue(board.handleMoveForGame(new Move("b8c6", board)));
        assertTrue(board.handleMoveForGame(new Move("d1a4", board)));
        assertTrue(board.handleMoveForGame(new Move("f8b4", board)));
        assertTrue(board.handleMoveForGame(new Move("c1d2", board)));
        assertTrue(board.handleMoveForGame(new Move("f6d5", board)));
        assertTrue(board.handleMoveForGame(new Move("d2b4", board)));
        assertTrue(board.handleMoveForGame(new Move("d5b4", board)));
        assertTrue(board.handleMoveForGame(new Move("a2a3", board)));
        assertTrue(board.handleMoveForGame(new Move("b7b5", board)));
        assertTrue(board.handleMoveForGame(new Move("a4b5", board)));
        assertTrue(board.handleMoveForGame(new Move("b4c2", board)));
        assertTrue(board.handleMoveForGame(new Move("e1f1", board)));
        assertTrue(board.handleMoveForGame(new Move("c8d7", board)));
        assertTrue(board.handleMoveForGame(new Move("a1a2", board)));
        assertTrue(board.handleMoveForGame(new Move("c2d4", board)));
        assertTrue(board.handleMoveForGame(new Move("b5c4", board)));
        assertTrue(board.handleMoveForGame(new Move("e6e5", board)));
        assertTrue(board.handleMoveForGame(new Move("f3d4", board)));
        assertTrue(board.handleMoveForGame(new Move("c6d4", board)));
        assertTrue(board.handleMoveForGame(new Move("c4d5", board)));
        assertTrue(board.handleMoveForGame(new Move("d7e6", board)));
        assertTrue(board.handleMoveForGame(new Move("d5a8", board)));
        assertTrue(board.handleMoveForGame(new Move("d8a8", board)));
        assertTrue(board.handleMoveForGame(new Move("g2a8", board)));
        assertTrue(board.handleMoveForGame(new Move("e6a2", board)));
        assertTrue(board.handleMoveForGame(new Move("b1c3", board)));
        assertTrue(board.handleMoveForGame(new Move("e8e7", board)));
        assertTrue(board.handleMoveForGame(new Move("c3a2", board)));
        assertTrue(board.handleMoveForGame(new Move("h8a8", board)));
        assertTrue(board.handleMoveForGame(new Move("e2e3", board)));
        assertTrue(board.handleMoveForGame(new Move("d4b3", board)));
        assertTrue(board.handleMoveForGame(new Move("f1e2", board)));
        assertTrue(board.handleMoveForGame(new Move("a7a5", board)));
        assertTrue(board.handleMoveForGame(new Move("h1d1", board)));
        assertTrue(board.handleMoveForGame(new Move("c7c6", board)));
        assertTrue(board.handleMoveForGame(new Move("e2d3", board)));
        assertTrue(board.handleMoveForGame(new Move("a8d8", board)));
        assertTrue(board.handleMoveForGame(new Move("d3c2", board)));
        assertTrue(board.handleMoveForGame(new Move("d8d1", board)));
        assertTrue(board.handleMoveForGame(new Move("c2d1", board)));
        assertTrue(board.handleMoveForGame(new Move("b3c5", board)));
        assertTrue(board.handleMoveForGame(new Move("d1c2", board)));
        assertTrue(board.handleMoveForGame(new Move("c5e4", board)));
        assertTrue(board.handleMoveForGame(new Move("f2f4", board)));
        assertTrue(board.handleMoveForGame(new Move("e7d6", board)));
        assertTrue(board.handleMoveForGame(new Move("b2b4", board)));
        assertTrue(board.handleMoveForGame(new Move("e5f4", board)));
        assertTrue(board.handleMoveForGame(new Move("e3f4", board)));
        assertTrue(board.handleMoveForGame(new Move("a5b4", board)));
        assertTrue(board.handleMoveForGame(new Move("a2b4", board)));
        assertTrue(board.handleMoveForGame(new Move("c6c5", board)));
        assertTrue(board.handleMoveForGame(new Move("b4d3", board)));
        assertTrue(board.handleMoveForGame(new Move("f7f6", board)));
        assertTrue(board.handleMoveForGame(new Move("f4f5", board)));
        assertTrue(board.handleMoveForGame(new Move("c5c4", board)));
        assertTrue(board.handleMoveForGame(new Move("d3f4", board)));
        assertTrue(board.handleMoveForGame(new Move("d6c6", board)));
        assertTrue(board.handleMoveForGame(new Move("f4e6", board)));
        assertTrue(board.handleMoveForGame(new Move("g7g5", board)));
        assertTrue(board.handleMoveForGame(new Move("f5g6", board)));
        System.out.println("Done");
    }

    @Test
    void testEnPassantInLegalMoves() {
        Board board = new Board();
        board.setupFromFen("8/7p/2k1Np2/5Pp1/2p1n3/P5P1/2K4P/8 w - g6 0 35");
        List<Move> moves = board.getLegalMoves();
        boolean foundExpectedMove = false;
        for (Move move : moves) {
            if (move.uciFormat().equals("f5g6")) {
                foundExpectedMove = true;
                break;
            }
        }

        assertTrue(foundExpectedMove);
    }

    @Test
    void checkAlgebraicTranslationOfAllMoves() {
        String fen = "r1bqk2r/1ppp2pp/pbn5/3nPpB1/2Bp4/1QP2N2/PP1N1PPP/R3K2R w KQkq f6 0 10";
        Board board = new Board();
        board.setupFromFen(fen);
        List<Move> legalMoves = board.getLegalMoves();
        NotationConverter converter = new NotationConverter(board);
        for (Move move : legalMoves) {
            converter.handleAlgebraicNotation(move.algebraicFormat());
            assertEquals(move.source().col, converter.sourcecol());
            assertEquals(move.source().row, converter.sourcerow());
            assertEquals(move.target().col, converter.targetcol());
            assertEquals(move.target().row, converter.targetrow());
        }
    }

    @Test
    public void getChat() {
        LichessChatItem[] allChat = LichessInterface.fetchGameChat("7D6QPrVI");
        for (int i = 0; i < allChat.length; ++i) {
            System.out.println("Text: " + allChat[i].text);
            System.out.println("User: " + allChat[i].user);
        }
    }

    @Test
    public void whyStalemate() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        String fen = "8/8/8/5Q2/8/k7/2Q4K/8 w - - 5 84";
        Board board = new Board();
        board.setupFromFen(fen);
        StockfishClient client = new StockfishClient();
        client.init(5000l, "test");
        Random random = new Random();
        GameLogger log = new GameLogger(Enums.LogLevel.DEBUG);
        for (int i = 0; i < 100; ++i) {
            MoveSelector moveSelector = new JustTheBestMoveSelector(random, Enums.EngineAlgorithm.JUST_THE_BEST, client, log, null);
            try {
                Move engineMove = moveSelector.selectMove(board);
                System.out.println(engineMove.algebraicFormat());
                assertTrue(engineMove != null &&
                                (engineMove.algebraicFormat().startsWith("Qa5") ||
                                        engineMove.algebraicFormat().startsWith("Qc5") ||
                                        engineMove.algebraicFormat().startsWith("Qf1")),
                        "Expected Qa5 or Qc5 or Qf1 but received " + engineMove.algebraicFormat());
            }
            catch (MoveSelectorException ex) {
                System.out.println("Got MoveSelectorException while calculating move");
            }
        }
    }

    @Test
    void checkStockfishReturnsAllPossibleMoves() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        String fen = "rnb1k1nr/pppp1ppp/3b1q2/4p3/1P4P1/5P2/P1PPP2P/RNBQKBNR w KQkq - 0 4";
        Board board = new Board();
        board.setupFromFen(fen);
        List<Move> legalMoves = board.getLegalMoves();
        StockfishClient client = new StockfishClient();
        client.init(5000l, "test");
        Random random = new Random();
        GameLogger log = new GameLogger(Enums.LogLevel.DEBUG);
        MoveSelector moveSelector = new BestWorstMoveSelector(random, Enums.EngineAlgorithm.WORST_MOVE, client, log, null);
        try {
            Move engineMove = moveSelector.selectMove(board);
        }
        catch (MoveSelectorException ex) {
            System.out.println("Got MoveSelectorException while calculating move");
        }
    }
*/
}
