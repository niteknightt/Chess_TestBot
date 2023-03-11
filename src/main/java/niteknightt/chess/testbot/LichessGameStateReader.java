package niteknightt.chess.testbot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import niteknightt.chess.common.AppLogger;
import niteknightt.chess.lichessapi.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class LichessGameStateReader implements Runnable {

    protected boolean _done = false;
    protected boolean _running = true;
    protected BotGame _game;

    public boolean done() { return _done; }
    public void setDone() { _done = true; }
    public void setNotDone() { _done = false; }

    public LichessGameStateReader(BotGame game) {
        _game = game;
    }

    public void handleGameStateString(String gameStateString) {
        JsonObject jobj = new Gson().fromJson(gameStateString, JsonObject.class);
        String messageType = jobj.get("type").getAsString();

        if ("gameFull".equals(messageType)) {
            _game.addEvent(new Gson().fromJson(gameStateString, LichessGameFullEvent.class));
        }
        else if ("gameState".equals(messageType)) {
            _game.addEvent(new Gson().fromJson(gameStateString, LichessGameStateEvent.class));
        }
        else if ("chatLine".equals(messageType)) {
            _game.addEvent(new Gson().fromJson(gameStateString, LichessChatLineEvent.class));
        }
        else if ("opponentGone".equals(messageType)) {
            _game.addEvent(new Gson().fromJson(gameStateString, LichessOpponentGoneEvent.class));
        }
        else {
            AppLogger.getInstance().error("Unknown game state type received: " + messageType);
        }
    }

    public void run() {
        URL url;
        URLConnection conn;
        try {
            url = new URL(LichessInterface.LICHESS_API_ENDPOINT_BASE + "bot/game/stream/" + _game.getGameId());
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Got MalformedURLException for this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "bot/game/stream/" + _game.getGameId());
        }
        try {
            conn = url.openConnection();
        }
        catch (IOException e) {
            throw new RuntimeException("Got IOException for this URL: " + LichessInterface.LICHESS_API_ENDPOINT_BASE + "bot/game/stream/" + _game.getGameId());
        }

        conn.setRequestProperty (LichessInterface.AUTH_KEY_TEXT, LichessInterface.AUTH_VALUE_TEXT);

        while (!done()) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    if (!inputLine.trim().isEmpty())
                        handleGameStateString(inputLine);
                in.close();

                try { Thread.sleep(5000); } catch (InterruptedException e) { }
            }
            catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().equals("stream is closed")) {
                    AppLogger.getInstance().error("Received exception while reading game state: " + e.toString());
                }
                if (e instanceof FileNotFoundException) {
                    continue;
                }
                AppLogger.getInstance().info("Game state stream is closed for game " + _game._gameId);
                setDone();
            }
        }
    }

}
