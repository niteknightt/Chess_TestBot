package niteknightt.chess.testbot.lichessevents;

import com.google.gson.Gson;
import niteknightt.chess.common.AppLogger;
import niteknightt.chess.lichessapi.LichessEnums;
import niteknightt.chess.lichessapi.LichessEvent;
import niteknightt.chess.lichessapi.LichessInterface;
import niteknightt.chess.testbot.BotManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class LichessEventReader implements Runnable {

    protected boolean _done = false;
    protected LichessEventHandler _eventHandler;
    protected Queue<LichessEvent> _events = new LinkedList<LichessEvent>();

    public boolean done() { return _done; }
    public void setDone() { _done = true; }
    public void setNotDone() { _done = false; }

    public LichessEventReader(LichessEventHandler eventHandler) {
        _eventHandler = eventHandler;
    }

    public LichessEvent peekQueue() {
        return _events.peek();
    }

    public LichessEvent popQueue() {
        return _events.remove();
    }

    public boolean isQueueEmpty() {
        return _events.isEmpty();
    }

    public void handleEventString(String eventString) {
        LichessEvent event = null;

        try {
            event = new Gson().fromJson(eventString, LichessEvent.class);
        }
        catch (Exception e) {
            AppLogger.getInstance().error("Exception while parsing event -- the event string is below");
            AppLogger.getInstance().error(eventString);
            AppLogger.getInstance().error(e.getMessage());
            //AppLogger.getInstance().error(e.getStackTrace());
        }

        if (event == null) {
            AppLogger.getInstance().error("Event is null -- event string is below");
            AppLogger.getInstance().error(eventString);
            return;
        }

        if (event.eventType == null) {
            AppLogger.getInstance().error("Event type is null -- event string is below");
            AppLogger.getInstance().error(eventString);
            return;
        }

        switch (event.eventType) {
            case CHALLENGE:
                _eventHandler.handleChallenge(event);
                break;
            case CHALLENGE_CANCELED:
                _eventHandler.handleChallengeCanceled(event);
                break;
            case CHALLENGE_DECLINED:
                _eventHandler.handleChallengeDeclined(event);
                break;
            case GAME_START:
                _eventHandler.handleGameStart(event);
                break;
            case GAME_FINISH:
                _eventHandler.handleGameFinish(event);
                break;
            default:
                throw new RuntimeException("Unknown Lichess event type received: " + event.eventType);
        }
    }

    public void run() {
        URL url;
        URLConnection conn;
        int connectionErrorCount = 0;
        Date lastConnectionErrorTime = null;

        try {
            url = new URL(LichessInterface.LICHESS_API_ENDPOINT_BASE + "stream/event");
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Got MalformedURLException for this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "stream/event");
        }
        try {
            conn = url.openConnection();
        }
        catch (IOException e) {
            throw new RuntimeException("Got IOException for this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "stream/event");
        }

        conn.setRequestProperty (LichessInterface.AUTH_KEY_TEXT, LichessInterface.AUTH_VALUE_TEXT);

        while (!done()) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null && !done())
                    if (!inputLine.trim().isEmpty())
                        handleEventString(inputLine);
                in.close();
                try { Thread.sleep(5000); } catch (InterruptedException e) { }
            }
            catch (IOException e) {
                Date now = new Date();
                if (lastConnectionErrorTime != null && Math.abs(now.getTime() - lastConnectionErrorTime.getTime()) > 60000) {
                    connectionErrorCount = 0;
                }

                ++connectionErrorCount;
                AppLogger.getInstance().error("Got IOException number " + connectionErrorCount + " while reading from this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "stream/event");

                if (connectionErrorCount >= 5) {
                    throw new RuntimeException("Got 5 IOExceptions while reading from this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "stream/event");
                }

                try { Thread.sleep(10000); } catch (InterruptedException ee) { }
            }
        }
        AppLogger.getInstance().info("Ending event reader thread");
    }

}
