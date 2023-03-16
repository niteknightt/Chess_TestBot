package niteknightt.chess.testbot;

import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessChallenge;
import niteknightt.chess.lichessapi.LichessInterface;

public class BotGameVsBot extends BotGame {

    public BotGameVsBot(LichessChallenge challenge) {
        super(challenge);
    }

    @Override
    protected void _performPregameTasks() {
        _setAlgorithm(Enums.EngineAlgorithm.JUST_THE_BEST);
        try {
            LichessInterface.writeChat(_gameId, "Hi! You're playing against @niteknighttbot, a bot created by @niteknightt!");
            LichessInterface.writeChat(_gameId, "GLHF!");
        }
        catch (LichessApiException e) { }
    }

    @Override
    protected void _performPostChallengerMoveTasks() {
        // Do nothing.
    }

    @Override
    protected void _performPostEngineMoveTasks() {
        // Do nothing.
    }

    /**
     * Handles text received from the challenger in the chat.
     *
     * @param text the text that the challenger wrote in the chat.
     */
    @Override
    protected void _handleChatFromChallenger(String text) {
        // Do nothing.
    }

    @Override
    protected void _performPostgameTasks() {
        try {
            LichessInterface.writeChat(_gameId, "GG!");
        }
        catch (LichessApiException e) { }
    }
}
