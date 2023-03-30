package niteknightt.chess.testbot;

import niteknightt.chess.common.*;
import niteknightt.chess.lichessapi.*;
import niteknightt.chess.testbot.lichessevents.LichessEventHandler;
import niteknightt.chess.testbot.lichessevents.LichessEventReader;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BotManager implements Runnable {

    public static int MAX_CONCURRENT_CHALLENGES = 5;

    protected Map<String, BotGame> _games;
    protected Map<String, Thread> _gameThreads;
    protected Map<String, Thread> _gameStateReaderThreads;
    protected Map<String, Date> _endedGames;

    protected LichessEventHandler _eventHandler;
    protected LichessEventReader _eventReader;
    protected Thread _eventReaderThread;

    protected boolean _done;
    protected Lock _mainLock = new ReentrantLock(true);

    protected int _numRunningChallenges;

    // Moved gamelogger to game -- new one created for each game.
    //protected GameLogger gameLogger = new GameLogger(Common.GAME_LOG_LEVEL);

    public BotManager() {
    }

    public void setNotDone() { _done = false; }
    public void setDone() { _done = true; }
    public boolean done() { return _done; }
    public int getNumRunningChallenges() { return _numRunningChallenges; }
    public void incrementNumRunningChallenges() { ++_numRunningChallenges; }

    public void init() {
        _games = new HashMap<String, BotGame>();
        _gameThreads = new HashMap<String, Thread>();
        _gameStateReaderThreads = new HashMap<String, Thread>();
        _endedGames = new HashMap<String, Date>();
        _numRunningChallenges = 0;

        _eventHandler = new LichessEventHandler(this);
        _eventReader = new LichessEventReader(_eventHandler);
        _eventReaderThread = new Thread(_eventReader);
        _eventReaderThread.start();

        setNotDone();
    }

    public void run() {
        init();

        while (!done()) {

            try {
                _mainLock.lock();

                Iterator<Map.Entry<String, Thread>> iter = _gameThreads.entrySet().iterator();
                // Check if any old completed games still has an open thread.
                while (iter.hasNext()) {
                    Map.Entry<String, Thread> pair = iter.next();
                    String gameId = pair.getKey();
                    Thread gameThread = pair.getValue();

                    if (gameThread == null || !gameThread.isAlive()) {
                        --_numRunningChallenges;

                        // Logging
                        StringBuilder sb = new StringBuilder()
                                .append("Removing game ")
                                .append(gameId)
                                .append(" -- ")
                                .append(_games.size() == 1 ? "no more games running" : (_games.size() - 1) + " games running:");
                        for (Map.Entry<String, Thread> game : _gameThreads.entrySet()) {
                            if (!game.getKey().equals(gameId)) {
                                sb.append(" ").append(game.getKey());
                            }
                        }
                        AppLogger.getInstance().info(sb.toString());

                        try {
                            iter.remove();
                            AppLogger.getInstance().info("Game " + gameId + " removed");
                        }
                        catch (Exception ex) {
                            AppLogger.getInstance().error("Exception while removing game thread: " + ex.toString());
                        }
                        try {
                            _games.remove(gameId);
                        }
                        catch (Exception ex) {
                            AppLogger.getInstance().error("Exception while removing game: " + ex.toString());
                        }
                        try {
                            _gameStateReaderThreads.remove(gameId);
                        }
                        catch (Exception ex) {
                            AppLogger.getInstance().error("Exception while removing game reader thread: " + ex.toString());
                        }
                    }
                }
            }
            catch (Exception ex) {
                AppLogger.getInstance().error("Exception while running main loop in BotManager: " + ex.toString());
            }
            finally {
                _mainLock.unlock();
            }

            try { Thread.sleep(500); } catch (InterruptedException interruptException) { }
        }

        AppLogger.getInstance().info("Closing BotManager");

        _eventReader.setDone();
        if (_eventReaderThread.isAlive()) {
            try { _eventReaderThread.join(15000); } catch (InterruptedException ex) {
                AppLogger.getInstance().warning("Failed to join event reader thread");
            }
        }
        else {
            AppLogger.getInstance().warning("Event reader thread is already dead");
        }

        // Remove any completed games.
        for (Map.Entry<String, BotGame> entry : _games.entrySet()) {
            String gameId = entry.getKey();
            BotGame game = entry.getValue();
            if (!game.done()) {
                game.forceEnd();
                AppLogger.getInstance().info("Forcing end to game " + gameId);
            }
        }

        try { Thread.sleep(500); } catch (InterruptedException interruptException) { }
    }

    public BotGame createGame(LichessChallenge challenge) {
        BotGame game = BotGame.createGameForChallenge(challenge);
        Thread gameThread = new Thread(game);
        gameThread.start();

        _games.put(challenge.id, game);
        _gameThreads.put(challenge.id, gameThread);

        return game;
    }

    public boolean startGame(String gameId) {
        if (_games.containsKey(gameId)) {
            BotGame game = _games.get(gameId);
            game.setStarted();

            LichessGameStateReader gameStateReader = new LichessGameStateReader(game);
            Thread gameStateReaderThread = new Thread(gameStateReader);
            gameStateReaderThread.start();
            _gameStateReaderThreads.put(gameId, gameStateReaderThread);

            return true;
        }
        else {
            return false;
        }
    }

    public boolean finishGame(String gameId) {
        if (_games.containsKey(gameId)) {
            _games.get(gameId).setFinished();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean forceGameEnd(LichessChallenge challenge) {
        if (_games.containsKey(challenge.id)) {
            BotGame game = _games.get(challenge.id);
            game.forceEnd();
            return true;
        }
        else {
            return false;
        }
    }
}
