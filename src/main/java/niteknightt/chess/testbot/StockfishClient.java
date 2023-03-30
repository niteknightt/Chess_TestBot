package niteknightt.chess.testbot;

import net.andreinc.neatchess.client.UciMod;
import net.andreinc.neatchess.client.model.Move;
import niteknightt.chess.common.Common;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.Helpers;
import niteknightt.chess.common.UciIoLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import net.andreinc.neatchess.client.Move;
//import net.andreing.neatchess.client.*;
//import niteknightt.chess.gameplay.Move;
//import net
// Most of this code is from: https://www.andreinc.net/2021/04/22/writing-a-universal-chess-interface-client-in-java
//import net.andreinc.neatchess.client.*;
//import niteknightt.chess.
//import niteknightt.chess.

//import niteknightt.chess.
public class StockfishClient {

    private Process process = null;
    private BufferedReader reader = null;
    private OutputStreamWriter writer = null;
    UciMod uci;
    String fen;
    long defaultTimeout;
    boolean startGameFlag = false;
    UciIoLogger logger = null;
    String _gameId = null;

    public StockfishClient() { }

    public void init(long defaultTimeout, String gameId) {
        this.defaultTimeout = defaultTimeout;
        logger = new UciIoLogger(Common.UCI_LOG_LEVEL);
        _gameId = gameId;
        uci = new UciMod(defaultTimeout, logger, gameId);
        uci.startStockfish();
    }

    public void startGame() {
        // do nothing.
        startGameFlag = true;
        uci.uciNewGame();
    }

    public void setPosition(String fen) {
        this.fen = fen;
        uci.positionFen(fen);
    }

    public List<EvaluatedMove> calcMoves(int numMoves, long timeoutMs, Enums.Color colorToMove) {
        uci.setOption("MultiPV", Integer.valueOf(numMoves).toString());
        var analysis = uci.analysis(10).getResultOrThrow();
        var moves = analysis.getAllMoves();
        if (moves.size() != numMoves) {
            logger.debugOutput(_gameId, "Num moves from UCI: " + moves.size() + " Num legal moves: " + numMoves);
            logger.debugOutput(_gameId, "Trying again");
            var analysis1 = uci.analysis(10).getResultOrThrow();
            var moves1 = analysis1.getAllMoves();
            logger.debugOutput(_gameId, "Retry Num moves from UCI: " + moves1.size() + " Num legal moves: " + numMoves);
        }
        List<EvaluatedMove> movesWithEval = new ArrayList<EvaluatedMove>();
        for (Map.Entry<Integer, Move> entry : moves.entrySet()) {
            String uciFormat = entry.getValue().getLan();
            Double eval = entry.getValue().getStrength().getScore();
            Enums.MoveEvalCategory category = Helpers.categoryFromEval(eval, colorToMove);

            double multiplier = (colorToMove == Enums.Color.WHITE ? 1.0 : -1.0);
            double testEval = eval * multiplier;


            int matein = 0;
            if (entry.getValue().getStrength().isForcedMate()) {
                matein = entry.getValue().getStrength().getMateIn();
            }
            int breakpoint = -1;
            for (int i = 0; i < movesWithEval.size(); ++i) {
                EvaluatedMove moveFromList = movesWithEval.get(i);
                if (colorToMove == Enums.Color.WHITE) {
                    if (matein > 0) {
                        if (!moveFromList.ismate) {
                            breakpoint = i;
                            break;
                        }
                        else if (moveFromList.matein > matein) {
                            breakpoint = i;
                            break;
                        }
                    }
                    else {
                        if (!moveFromList.ismate) {
                            if (colorToMove == Enums.Color.WHITE && moveFromList.eval < eval.doubleValue()) {
                                breakpoint = i;
                                break;
                            }
                            if (colorToMove == Enums.Color.BLACK && moveFromList.eval > eval.doubleValue()) {
                                breakpoint = i;
                                break;
                            }
                        }
                    }
                }
            }
            EvaluatedMove newmove = new EvaluatedMove();
            newmove.uci = uciFormat;
            newmove.eval = eval;
            newmove.evalCategory = category;
            newmove.matein = matein;
            newmove.ismate = (matein > 0);
            newmove.continuation = entry.getValue().getContinuation();
            if (breakpoint == -1) {
                movesWithEval.add(newmove);
            }
            else {
                movesWithEval.add(breakpoint, newmove);
            }
        }

        if (moves.size() != movesWithEval.size()) {
            logger.debugOutput(_gameId, "Mismatch: " + moves.size() + " vs " + movesWithEval.size());
        }

        return movesWithEval;
    }

    public String calcBestMove(int depth, long timeoutMs) {
        var result = uci.bestMove(depth).getResultOrThrow();
        return result.getCurrent();
    }

    public void start(String cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            this.process = pb.start();
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.writer = new OutputStreamWriter(process.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (this.process.isAlive()) {
            this.process.destroy();
        }
        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.close();
    }
}
