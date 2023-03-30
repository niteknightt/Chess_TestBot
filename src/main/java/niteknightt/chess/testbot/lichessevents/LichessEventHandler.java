package niteknightt.chess.testbot.lichessevents;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessEnums;
import niteknightt.chess.lichessapi.LichessEvent;
import niteknightt.chess.lichessapi.LichessInterface;
import niteknightt.chess.testbot.BotGame;
import niteknightt.chess.testbot.BotManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LichessEventHandler {

    protected BotManager _botManager;
    protected Lock _mainLock = new ReentrantLock(true);

    public LichessEventHandler(BotManager botManager) {
        _botManager = botManager;
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
                if (_botManager.getNumRunningChallenges() >= BotManager.MAX_CONCURRENT_CHALLENGES) {
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
                if (event.challenge.challenger.title != null && event.challenge.challenger.title.equals("BOT")) {
                    // Challenge from bot - decline
                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because it is a from a bot.");
                    LichessInterface.declineChallenge(event.challenge.id, "noBot");
                    return;
                }
                if ((event.challenge.challenger.title == null || !event.challenge.challenger.title.equals("BOT")) &&
                        !event.challenge.challenger.id.equalsIgnoreCase("flowerhd") &&
                        !event.challenge.challenger.id.equalsIgnoreCase("niteknightt")) {
                    // Rated challenge from human - decline
                    AppLogger.getInstance().info("Declining challenge ID " + event.challenge.id + " because it is not from one of us.");
                    LichessInterface.declineChallenge(event.challenge.id, "later");
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

            _botManager.incrementNumRunningChallenges();
            BotGame game = _botManager.createGame(event.challenge);

            try {
                LichessInterface.acceptChallenge(event.challenge.id);
            }
            catch (LichessApiException e) {
                AppLogger.getInstance().error("Got LichessApiException while trying to accept challenge");
                game.setFinished();
                return;
            }

            AppLogger.getInstance().info("Accepted challenge ID " + event.challenge.id + " -- currently there are " + _botManager.getNumRunningChallenges() + " running games");
        }
        finally {
            _mainLock.unlock();
        }
    }

    public void handleChallengeCanceled(LichessEvent event) {
        try {
            _mainLock.lock();

            if (_botManager.forceGameEnd(event.challenge)) {
                AppLogger.getInstance().info("Got cancellation of Canceled challenge ID " + event.challenge.id + " -- currently there are " + _botManager.getNumRunningChallenges() + " running games");
            }
            else {
                AppLogger.getInstance().error("Got cancellation of challenge ID " + event.challenge.id + " but it is not in the list of games");
            }
        }
        finally {
            _mainLock.unlock();
        }
    }

    public void handleChallengeDeclined(LichessEvent event) {
        if (event.challenge.challenger.id.equals(BotManager.BOT_NAME)) {
            // This is just a notification of the decline that we actually did.
            return;
        }
        AppLogger.getInstance().error("Got challenge declined for challenge ID " + event.challenge.id + " challengee " + event.challenge.challengee.id + " reason " + event.challenge.declineReason);
    }

    public void handleGameStart(LichessEvent event) {
        try {
            _mainLock.lock();

            if (_botManager.startGame(event.game.id)) {
                AppLogger.getInstance().info("Got game start event for game ID " + event.game.id);
            }
            else {
                AppLogger.getInstance().error("Got game start event for game ID " + event.game.id + " but it is not in the list of games");
                try {
                    LichessInterface.writeChat(event.game.id, "Lichess tells me I'm supposed to play this game, but I don't recognize it, so I am resigning. Bye.");
                }
                catch (LichessApiException e) { // do nothing
                }
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

            if (_botManager.finishGame(event.game.id)) {
                AppLogger.getInstance().info("Got game finish event for game ID " + event.game.id);
            }
            else {
                AppLogger.getInstance().error("Got game finish event for game ID " + event.game.id + " but it is not in the list of games");
                try {
                    LichessInterface.writeChat(event.game.id, "Lichess tells me I'm supposed to finish this game, but I don't recognize it, so I am resigning. Bye.");
                }
                catch (LichessApiException e) { // do nothing
                }
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
