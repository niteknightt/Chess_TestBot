package niteknightt.chess.testbot;

import niteknightt.chess.common.*;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessEnums;
import niteknightt.chess.lichessapi.LichessEvent;
import niteknightt.chess.lichessapi.LichessInterface;

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

    public void init() {
        _games = new HashMap<String, BotGame>();
        _gameThreads = new HashMap<String, Thread>();
        _gameStateReaderThreads = new HashMap<String, Thread>();
        _endedGames = new HashMap<String, Date>();
        _numRunningChallenges = 0;
        _eventReader = new LichessEventReader(this);
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

    public void handleChallenge(LichessEvent event) {
        try {
            _mainLock.lock();

            try {
                if (!event.challenge.variant.key.equals(LichessEnums.VariantKey.STANDARD)) {
                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because it was not standard chess.");
                    LichessInterface.declineChallenge(event.challenge.id, "standard");
                    return;
                }
                if (_numRunningChallenges >= MAX_CONCURRENT_CHALLENGES) {
                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because there are too many games in progress.");
                    LichessInterface.declineChallenge(event.challenge.id, "later");
                    return;
                }
                if ((event.challenge.challenger.title == null || !event.challenge.challenger.title.equals("BOT")) &&
                        event.challenge.rated) {
                    // Rated challenge from human - decline
                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because it is a rated challenge from a human.");
                    LichessInterface.declineChallenge(event.challenge.id, "casual");
                    return;
                }

                // UNCOMMENT THIS CODE WHILE WORKING ON THE PROGRAM.
                // COMMENT IT WHEN YOU WANT THE BOT TO ACCEPT CHALLENGES.

//                if (((event.challenge.challenger.title != null && event.challenge.challenger.title.equals("BOT"))) || !event.challenge.challenger.id.equals("niteknightt")) {
//                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because not accepting challenges at this time.");
//                    LichessInterface.declineChallenge(event.challenge.id, "later");
//                    return;
//                }
            }
            catch (LichessApiException e) {
                AppLogger.getInstance().error("Got LichessApiException while trying to decline challenge");
            }

            ++_numRunningChallenges;

            BotGame game = BotGame.createGameForChallenge(event.challenge);
            Thread gameThread = new Thread(game);
            gameThread.start();

            _games.put(event.challenge.id, game);
            _gameThreads.put(event.challenge.id, gameThread);

            try {
                LichessInterface.acceptChallenge(event.challenge.id);
            }
            catch (LichessApiException e) {
                AppLogger.getInstance().error("Got LichessApiException while trying to accept challenge");
                game.setFinished();
                return;
            }

            AppLogger.getInstance().info("Accepted challenge ID " + event.challenge.id + " -- currently there are " + _numRunningChallenges + " running games");
        }
        finally {
            _mainLock.unlock();
        }
    }

    public void handleChallengeCanceled(LichessEvent event) {
        try {
            _mainLock.lock();

            if (_games.containsKey(event.challenge.id)) {
                BotGame game = _games.get(event.challenge.id);
                game.forceEnd();
                AppLogger.getInstance().info("Canceled challenge ID " + event.challenge.id + " -- currently there are " + _numRunningChallenges + " running games");
            }
            else {
                AppLogger.getInstance().error("Got cancelation of challenge ID " + event.challenge.id + " but it is not in the list of games");
            }
        }
        finally {
            _mainLock.unlock();
        }
    }

    public void handleChallengeDeclined(LichessEvent event) {
        AppLogger.getInstance().error("Got challenge declined for challenge ID " + event.challenge.id + " -- don't know what to do with this");
    }

    public void handleGameStart(LichessEvent event) {
        try {
            _mainLock.lock();

            if (_games.containsKey(event.game.id)) {
                BotGame game = _games.get(event.game.id);
                game.setStarted();

                LichessGameStateReader gameStateReader = new LichessGameStateReader(game);
                Thread gameStateReaderThread = new Thread(gameStateReader);
                gameStateReaderThread.start();
                _gameStateReaderThreads.put(event.game.id, gameStateReaderThread);

                AppLogger.getInstance().info("Got game start event for game ID " + event.game.id);
            }
            else {
                AppLogger.getInstance().error("Got game start event for game ID " + event.game.id + " but it is not in the list of games");
                try {
                    LichessInterface.writeChat(event.game.id, "Lichess tells me I'm supposed to play this game, but I don't recognize it, so I am resigning. Bye.");
                }
                catch (LichessApiException e) { }
                try {
                    LichessInterface.resignGame(event.game.id);
                }
                catch (LichessApiException e) {
                    AppLogger.getInstance().warning("Got LichessApiException while trying to resign a game that I could not start");
                }
            }
        }
        finally {
            _mainLock.unlock();
        }
    }

    public void handleGameFinish(LichessEvent event) {
        try {
            _mainLock.lock();

            if (_games.containsKey(event.game.id)) {
                _games.get(event.game.id).setFinished();
                AppLogger.getInstance().info("Got game finish event for game ID " + event.game.id);
            }
            else {
                AppLogger.getInstance().error("Got game finish event for game ID " + event.game.id + " but it is not in the list of games");
                try {
                    LichessInterface.writeChat(event.game.id, "Lichess tells me I'm supposed to finish this game, but I don't recognize it, so I am resigning. Bye.");
                }
                catch (LichessApiException e) { }
                try {
                    LichessInterface.resignGame(event.game.id);
                }
                catch (LichessApiException e) {
                    AppLogger.getInstance().warning("Got LichessApiException while trying to resign a game that I could not finish");
                }
            }
        }
        finally {
            _mainLock.unlock();
        }
    }
}
