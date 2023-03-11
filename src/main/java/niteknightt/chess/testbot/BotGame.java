package niteknightt.chess.testbot;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import niteknightt.chess.common.Constants;
import niteknightt.chess.common.Helpers;
import niteknightt.chess.testbot.moveselectors.BestWorstMoveSelector;
import niteknightt.chess.testbot.moveselectors.InstructiveMoveSelector;
import niteknightt.chess.testbot.moveselectors.JustTheBestMoveSelector;
import niteknightt.chess.testbot.moveselectors.MoveSelector;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;
import niteknightt.chess.lichessapi.*;

public abstract class BotGame implements Runnable {

    public static Enums.LogLevel GAME_LOG_LEVEL = Enums.LogLevel.DEBUG;

    protected String _gameId;
    protected Enums.GameState _gameState;
    protected LichessChallenge _challenge;
    protected StockfishClient _stockfishClient = new StockfishClient();
    protected Board _board;
    protected int _numMovesPlayedByChallenger;
    protected int _numMovesPlayedByEngine;
    protected Date _lastGameStateUpdate;
    protected Enums.Color _engineColor;
    protected Enums.Color _challengerColor;
    protected Enums.EngineAlgorithm _algorithm;
    protected MoveSelector _moveSelector;
    protected Random _random = new Random();
    protected List<Move> _moves = new ArrayList<Move>();
    protected Queue<LichessGameEvent> _events = new LinkedList<LichessGameEvent>();
    protected Lock _eventLock = new ReentrantLock(true);
    protected GameLogger _gameLogger = null;

    public static BotGame createGameForChallenge(LichessChallenge challenge) {
        Helpers.setGameStartDate();
        if (challenge.challenger.title != null && challenge.challenger.title.equals("BOT")) {
            return new BotGameVsBot(challenge);
        }
        else {
            return new BotGameVsHuman(challenge);
        }
    }

    public BotGame(LichessChallenge challenge) {
        _challenge = challenge;
        _gameId = challenge.id;
        _gameLogger = new GameLogger(GAME_LOG_LEVEL);
        _gameLogger.debug(_gameId, "general", "Game created");

        initGame();
    }

    /**
     * Initializes data structures after accepting a challenge from another Lichess user.
     */
    protected void initGame() {
        setGameState(Enums.GameState.CREATED);

        _stockfishClient.init(5000l, _gameId);
        _stockfishClient.startGame();

        _board = new Board();
        _board.setupStartingPosition();

        _numMovesPlayedByEngine = 0;
        _numMovesPlayedByChallenger = 0;
    }

    public Enums.GameState gameState() { return _gameState; }

    public void setGameState(Enums.GameState gameState ) {
        _gameState = gameState; _lastGameStateUpdate = new Date();
        _gameLogger.debug(_gameId, "state", "Set gamestate to " + _gameState.toString());
    }

    /**
     * Initializes game items after receiving the full game state from Lichess.
     * This should happen once per game after starting up the bot.
     */
    protected void _handleReceivingFullGameState(String whitePlayerId, String blackPlayerId) {
        if (blackPlayerId.equals("niteknighttbot")) {
            _engineColor = Enums.Color.BLACK;
            _challengerColor = Enums.Color.WHITE;
        }
        else {
            _engineColor = Enums.Color.WHITE;
            _challengerColor = Enums.Color.BLACK;
        }
        _gameLogger.debug(_gameId, "game", "Engine is " + _engineColor.toString());

        setGameState(Enums.GameState.FULL_STATE_RECEIVED);

        _performPregameTasks();
    }

    protected abstract void _performPregameTasks();

    protected void _setAlgorithm(Enums.EngineAlgorithm algorithm) {
        _algorithm = algorithm;
        _gameLogger.debug(_gameId, "game", "Set algorithm to " + _algorithm.toString());
        _setMoveSelector();
    }

    protected void _setMoveSelector() {
        if (_algorithm == Enums.EngineAlgorithm.BEST_MOVE || _algorithm == Enums.EngineAlgorithm.WORST_MOVE) {
            _moveSelector = new BestWorstMoveSelector(_random, _algorithm, _stockfishClient, _gameLogger, _gameId);
        }
        else if (_algorithm == Enums.EngineAlgorithm.INSTRUCTIVE) {
            _moveSelector = new InstructiveMoveSelector(_random, _algorithm, _stockfishClient, _gameLogger, _gameId);
        }
        else if (_algorithm == Enums.EngineAlgorithm.JUST_THE_BEST) {
            _moveSelector = new JustTheBestMoveSelector(_random, _algorithm, _stockfishClient, _gameLogger, _gameId);
        }
        else {
            _gameLogger.error(_gameId, "game", "When choosing move selector, the algorithm (" + _algorithm.toString() + ") was not handled");
            _handleErrorInGame(true, false, Enums.GameState.ABORTED, "This is embarrassing. I can't figure out which method to use to select moves, so I have to quit the game. Sorry!");
        }
    }

    /**
     * Handle receiving a challenger's move, and verify that our internal state of the game
     * matches Lichess's state.
     *
     * @param status the status of the game according to Lichess.
     * @param moves list of all moves played in the game according to Lichess.
     */
    protected void _handleReceivingIncrementalGameState(LichessEnums.GameStatus status, String moves) {

        // Log information about the game state received.
        if (status.equals(LichessEnums.GameStatus.ABORTED) ||
                status.equals(LichessEnums.GameStatus.DRAW) ||
                status.equals(LichessEnums.GameStatus.MATE) ||
                status.equals(LichessEnums.GameStatus.OUT_OF_TIME) ||
                status.equals(LichessEnums.GameStatus.RESIGN) ||
                status.equals(LichessEnums.GameStatus.STALEMATE) ||
                status.equals(LichessEnums.GameStatus.TIMEOUT) ||
                status.equals(LichessEnums.GameStatus.UNKNOWN_FINISH)){
            _gameLogger.info(_gameId, "game", "Received game-ending event -- not doing any moves");
            return;
        }

        // Make the challenger's move that is in the game state, if there is one.
        String moveSRs[] = moves.split(" ");
        if (_board.whosTurnToGo() == _challengerColor && moveSRs.length == _moves.size() + 1) {
            Move currentMove = new Move(moveSRs[moveSRs.length - 1], _board);

            _gameLogger.info(_gameId, "game", "Challenger's move is " + currentMove.algebraicFormat());

            if (!_board._isMoveLegal(currentMove)) {
                _gameLogger.error(_gameId, "game", "Before trying the challenger's move, the board says it is illegal -- Move: " + currentMove.algebraicFormat() + " Fen: " + _board.getFen());
                setGameState(Enums.GameState.ERROR);
            }
            else {
                if (!_board.handleMoveForGame(currentMove)) {
                    _gameLogger.error(_gameId, "game", "The board returned false when trying to play the challenger's move -- Move: " + currentMove.algebraicFormat() + " Fen: " + _board.getFen());
                    setGameState(Enums.GameState.ERROR);
                }
                _moves.add(currentMove);
                ++_numMovesPlayedByChallenger;
                _gameLogger.debug(_gameId, "game", "Did challenger's move " + currentMove.algebraicFormat() + " fen is now " + _board.getFen());

                _performPostmoveTasks();
            }
        }
    }

    protected abstract void _performPostmoveTasks();

    public Board getBoard() { return _board; }

    public Move getLastMove() { return _moves.get(_moves.size() - 1); }

    public MoveSelector getMoveSelector() { return _moveSelector; }

    public String getGameId() { return _gameId; }

    /**
     * Handles text received from the challenger in the chat.
     *
     * @param text the text that the challenger wrote in the chat.
     */
    protected abstract void _handleChatFromChallenger(String text);

    /**
     * Selects a move for the engine to play, and plays it.
     */
    protected void _playEngineMove() {
        _gameLogger.debug(_gameId, "game", "About to play engine move while fen is: " + _board.getFen());
        Move engineMove = null;

        try {
            engineMove = _moveSelector.selectMove(_board);
        }
        catch (MoveSelectorException e) {
            _handleErrorInGame(true, false, Enums.GameState.ABORTED, "Sorry dude, I'm quitting this game because I'm not sure how to play right now.");
            return;
        }

        if (engineMove == null) {
            _gameLogger.error(_gameId, "game", "Ending game internally because no legal moves for engine");
            _handleErrorInGame(false, false, Enums.GameState.ENDED_INTERNALLY, "Looks like I have no legal moves, but Lichess hasn't informed me yet that the game is over. Weird.");
            return;
        }

        _gameLogger.info(_gameId, "game", "Engine's move is " + engineMove.algebraicFormat());

        if (!_board.handleMoveForGame(engineMove)) {
            _gameLogger.error(_gameId, "game", "The board returned false when trying to play the engine's move -- Move: " + engineMove.algebraicFormat() + " Fen: " + _board.getFen());
            _handleErrorInGame(false, false, Enums.GameState.ERROR, "The computer won't let me make the move I want to make. Looks like I have to quit. Sorry.");
        }
        _gameLogger.debug(_gameId, "mainloop", "Played engine move " + engineMove.algebraicFormat() + " fen is " + _board.getFen());

        _moves.add(engineMove);
        ++_numMovesPlayedByEngine;
        try {
            LichessInterface.makeMove(_gameId, engineMove.uciFormat());
        }
        catch (LichessApiException e) {
            _gameLogger.error(_gameId, "comm", "Caught LichessApiException while sending move to Lichess");
            _handleErrorInGame(false, false, Enums.GameState.ERROR, "I want to make a move, but can't seem to send it to Lichess. Gotta quit.");
        }
    }

    /**
     * Main loop of the game.
     */
    public void run() {

        while (!done()) {

            while (_handleNextEvent()) {
                // Just do the loop.
            }

            if (gameState() == Enums.GameState.FULL_STATE_RECEIVED) {
                if (_board.whosTurnToGo() == _engineColor) {
                    _gameLogger.debug(_gameId, "mainloop", "Playing engine move fen is " + _board.getFen());
                    _playEngineMove();
                }
            }
            else if (gameState() == Enums.GameState.ERROR) {
                // This state can occur if any unexpected situation is caught by the program.
                // Only the program sets this state.
                _gameLogger.error(_gameId, "mainloop", "Gamestate is in error -- aborting game");
                _handleErrorInGame(true, false, Enums.GameState.ABORTED, "Hey guess what? Some sort of error happened in my program and I have to quit the game. Sorry!");
            }
            else if (gameState() == Enums.GameState.EXTERNAL_FORCED_END) {
                // This might happen if the bot manager decided it has to kill the game.
                _gameLogger.info(_gameId, "mainloop", "Received external forced end -- aborting game");
                _handleErrorInGame(true, false, Enums.GameState.ABORTED, "OMG! I've been instructed by the program to quit the gane. Bye for now.");
            }
            else if (gameState() == Enums.GameState.ABORTED) {
                // I am using this to catch the case where the opponent has left the game.
                // We should add code to claim victory.
                _gameLogger.info(_gameId, "mainloop", "Received game abort -- aborting game");
                _handleErrorInGame(false, true, Enums.GameState.ABORTED, "Wow, it looks like you left. Bye for now.");
            }
            else if (gameState() == Enums.GameState.CREATED ||
                    gameState() == Enums.GameState.STARTED_BY_EVENT ||
                    gameState() == Enums.GameState.ENDED_INTERNALLY) {
                // These are states that should only be very temporary. So if we are
                // stuck in one of them, apparently something bad has happened and
                // we should kill the game.
                // CREATED -- challenge has been received but GAME_START event not yet received
                // STARTED_BY_EVENT -- GAME_START event received but full game state data not yet received
                // ENDED_INTERNALLY -- program detected game end (mate, etc.) but GAME_FINISH event not yet received
                Date now = new Date();
                long diffInMillies = Math.abs(now.getTime() - _lastGameStateUpdate.getTime());
                if (diffInMillies > 15000) {
                    _gameLogger.error(_gameId, "mainloop", "Game state (" + _gameState.toString() + ") unchanged for 15 seconds -- aborting game");
                    _handleErrorInGame(true, false, Enums.GameState.ABORTED, "I zoned out for just a second and now I'm totally confused about this game. I gotta go.");
                }
            }

            try { Thread.sleep(100); } catch (InterruptedException interruptException) { }
        }

        _performPostgameTasks();
    }

    protected void _handleOpponentGone() {
        // Using ABORTED for now. Should add new state for OPPONENT_GONE.
        setGameState(Enums.GameState.ABORTED);
    }

    protected void _handleErrorInGame(boolean doQuitGame, boolean claimVictory, Enums.GameState newGameState, String textForChat) {

        try {
            if (textForChat != null) {
                LichessInterface.writeChat(_gameId, textForChat);
            }
        }
        catch (LichessApiException e) { }
        try {
            if (doQuitGame) {
                LichessInterface.resignGame(_gameId);
            }
        }
        catch (LichessApiException e) { }
        try {
            if (claimVictory) {
                LichessInterface.claimVictory(_gameId);
            }
        }
        catch (LichessApiException e) { }

        setGameState(newGameState);
    }

    public boolean done() {
        if (gameState() == Enums.GameState.FINISHED_BY_EVENT || gameState() == Enums.GameState.ABORTED) {
            _gameLogger.info(_gameId, "game", "Ending main loop because game state is " + gameState());
            return true;
        }
        return false;
    }

    protected abstract void _performPostgameTasks();

    /**
     * Handles receiving a "gameFull" state from the game state stream.
     * @param event the event data of the state.
     */
    protected void sendFullGameState(LichessGameFullEvent event) {
        _handleReceivingFullGameState(event.white.id, event.black.id);
    }

    /**
     * Handles receiving a "gameState" state from the game state stream.
     * @param event the event data of the state.
     */
    protected void sendCurrentGameState(LichessGameStateEvent event) {
        _handleReceivingIncrementalGameState(event.status, event.moves);
    }

    /**
     * Handles receiving a "chatLine" state from the game state stream.
     * @param event the event data of the state.
     */
    protected void sendChatMessage(LichessChatLineEvent event) {
        if (event.username.equals("niteknighttbot")) {
            _gameLogger.debug(_gameId, "event", "Received chat message that the bot sent");
            return;
        }
        _gameLogger.info(_gameId, "event", "Received chat message from challenger");
        _handleChatFromChallenger(event.text);
    }

    /**
     * Handles receiving a "opponentGone" state from the game state stream.
     * @param event the event data of the state.
     */
    protected void sendOpponentGone(LichessOpponentGoneEvent event) {
        if (event.gone) {
            _gameLogger.info(_gameId, "event", "Received chat message that opponent has left");
            _handleOpponentGone();
        }
    }

    public void addEvent(LichessGameEvent event) {
        try {
            _eventLock.lock();
            _events.add(event);
            _gameLogger.info(_gameId, "event", "Added event " + event.getClass().toString());
        }
        catch (Exception ex) {

        }
        finally {
            _eventLock.unlock();
        }
    }

    public boolean _handleNextEvent() {
        try {
            _eventLock.lock();
            if (_events.isEmpty()) {
                return false;
            }

            LichessGameEvent event = _events.remove();
            _gameLogger.info(_gameId, "event", "Handling event " + event.getClass().toString());

            if (event instanceof LichessGameFullEvent) {
                sendFullGameState((LichessGameFullEvent)event);
            }
            else if (event instanceof LichessGameStateEvent) {
                sendCurrentGameState((LichessGameStateEvent)event);
            }
            else if (event instanceof LichessChatLineEvent) {
                sendChatMessage((LichessChatLineEvent)event);
            }
            else if (event instanceof LichessOpponentGoneEvent) {
                sendOpponentGone((LichessOpponentGoneEvent)event);
            }
            else {
                throw new RuntimeException("Wrong type of event: " + event.getClass().getName());
            }
            return true;
        }
        catch (Exception ex) {
            _gameLogger.error(_gameId, "event", "Exception while handling event: " + ex.toString());
            return false;
        }
        finally {
            _eventLock.unlock();
        }
    }

    public void setStarted() {
        setGameState(Enums.GameState.STARTED_BY_EVENT);
    }

    public void setFinished() {
        setGameState(Enums.GameState.FINISHED_BY_EVENT);
    }

    public void forceEnd() {
        setGameState(Enums.GameState.EXTERNAL_FORCED_END);
    }
}
