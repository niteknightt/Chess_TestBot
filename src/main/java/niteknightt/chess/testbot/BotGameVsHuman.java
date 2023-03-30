package niteknightt.chess.testbot;

import niteknightt.chess.common.Enums;
import niteknightt.chess.lichessapi.LichessApiException;
import niteknightt.chess.lichessapi.LichessChallenge;
import niteknightt.chess.lichessapi.LichessInterface;
import niteknightt.chess.testbot.moveselectors.InstructiveMoveSelector;

import java.util.ArrayList;
import java.util.List;

public class BotGameVsHuman extends BotGame {

    public static Enums.EngineAlgorithm DEFAULT_ALGORITHM = Enums.EngineAlgorithm.BEST_MOVE;

    protected List<PotentialMoves> _allPotentialMoves = new ArrayList<>();
    protected List<PotentialMoves> _enginePotentialMoves = new ArrayList<>();
    protected List<PotentialMoves> _humanPotentialMoves = new ArrayList<>();

    public BotGameVsHuman(LichessChallenge challenge) {
        super(challenge);
    }

    @Override
    protected void _performPregameTasks() {
        _setAlgorithmFromChallengerProps();
        _writeWelcomeToChallenger();

        if (_engineColor == Enums.Color.BLACK) {
            List<EvaluatedMove> nextHumanMoves =  _moveSelector.getAllMoves(_board);
            PotentialMoves potentialMoves = new PotentialMoves(nextHumanMoves, _board);
            _allPotentialMoves.add(potentialMoves);
            _humanPotentialMoves.add(potentialMoves);
        }
    }

    /**
     * Checks if there is a saved algorithm for this challenger.
     * If there is, and it is not NONE, use it for the engine.
     * If it is NONE, use the default algorithm for the engine.
     * If there are no properties for this challenger, create properties
     * and set the algorithm in the properties to NONE while using the
     * default algorithm for the engine.
     */
    protected void _setAlgorithmFromChallengerProps() {
        OpponentProperties props = OpponentProperties.getForOpponent(_challenge.challenger.id);
        if (props != null && props.algorithm != Enums.EngineAlgorithm.NONE) {
            _setAlgorithm(props.algorithm);
        }
        else {
            _setAlgorithm(DEFAULT_ALGORITHM);

            if (props == null) {
                OpponentProperties.createOrUpdateAlgorithmForOpponent(_challenge.challenger.id, Enums.EngineAlgorithm.NONE);
            }
        }
    }

    protected void _writeWelcomeToChallenger() {
        OpponentProperties props = OpponentProperties.getForOpponent(_challenge.challenger.id);
        try {
            if (props != null && props.algorithm != Enums.EngineAlgorithm.NONE) {
                LichessInterface.writeChat(_gameId, "Welcome back " + _challenge.challenger.id + "! I will be using algorithm " + _algorithm + " since that is what you have set up already.");
            }
            else {
                if (props == null) {
                    LichessInterface.writeChat(_gameId, "Welcome " + _challenge.challenger.id + "! Since you have never played me before, I will be using algorithm " + _algorithm + " which is my default algorithm.");
                }
                else {
                    LichessInterface.writeChat(_gameId, "Welcome back " + _challenge.challenger.id + "! I will be using algorithm " + _algorithm + ", my default algorithm, since you haven't set up an algorithm yet.");
                }
            }
        }
        catch (LichessApiException e) { }
    }

    @Override
    protected void _performPostChallengerMoveTasks() {
        List<EvaluatedMove> nextEngineMoves =  _moveSelector.getAllMoves(_board);
        PotentialMoves potentialMoves = new PotentialMoves(nextEngineMoves, _board);
        _allPotentialMoves.add(potentialMoves);
        _enginePotentialMoves.add(potentialMoves);

        if (_algorithm == Enums.EngineAlgorithm.INSTRUCTIVE) {
            if (!Instructor.reviewLastHumanMove(this)) {
                setGameState(Enums.GameState.ERROR);
            }
        }
    }

    @Override
    protected void _performPostEngineMoveTasks() {
        List<EvaluatedMove> nextHumanMoves =  _moveSelector.getAllMoves(_board);
        PotentialMoves potentialMoves = new PotentialMoves(nextHumanMoves, _board);
        _allPotentialMoves.add(potentialMoves);
        _humanPotentialMoves.add(potentialMoves);

        String opportunityString = "";
        if (_algorithm == Enums.EngineAlgorithm.INSTRUCTIVE) {
            if (((InstructiveMoveSelector)_moveSelector).isOpportunityForHuman()) {
                opportunityString = " with opportunity";
                try {
                    LichessInterface.writeChat(_gameId, "Opportunities await!");
                }
                catch (LichessApiException ex2) { }
            }
        }

        _gameLogger.debug(_gameId, "moveselector", "Potential moves for challenger: " + potentialMoves + opportunityString);
    }

    /**
     * Handles text received from the challenger in the chat.
     *
     * @param text the text that the challenger wrote in the chat.
     */
    @Override
    protected void _handleChatFromChallenger(String text) {
        if (text.startsWith("algo")) {
            // Only do this part if the chat line starts with "algo"
            String remainingText = text.substring("algo".length());
            try {
                int algoCode = Integer.parseInt(remainingText.trim());
                if (algoCode < 0 || algoCode > 3) {
                    _gameLogger.info(_gameId, "event", "Invalid algo choice: " + text);
                    LichessInterface.writeChat(_gameId, "Sorry, only algos 0, 1, 2, and 3 are working so far.");
                    return;
                }
                Enums.EngineAlgorithm requestedAlgorithm = Enums.EngineAlgorithm.fromValue(algoCode);
                OpponentProperties.createOrUpdateAlgorithmForOpponent(_challenge.challenger.id, requestedAlgorithm);
                Enums.EngineAlgorithm prevAlgoritm = _algorithm;
                if (requestedAlgorithm == Enums.EngineAlgorithm.NONE) {
                    _setAlgorithm(DEFAULT_ALGORITHM);
                    if (prevAlgoritm != _algorithm) {
                        LichessInterface.writeChat(_gameId, "Switched to the default algorithm, " + DEFAULT_ALGORITHM + ", because the user set algoritm choice to NONE");
                    }
                }
                else {
                    _setAlgorithm(requestedAlgorithm);
                    if (prevAlgoritm != _algorithm) {
                        LichessInterface.writeChat(_gameId, "Switched to the " + _algorithm + " algorithm because the user requested it");
                    }
                }
                _gameLogger.info(_gameId, "event", "User sent valid algo choice: " + text);
            }
            catch (LichessApiException ex) { }
            catch (NumberFormatException e) {
                _gameLogger.info(_gameId, "event", "Failed to parse algo choice: " + text);
                try {
                    LichessInterface.writeChat(_gameId, "Yeah, I couldn't understand that. But we're cool, right?");
                }
                catch (LichessApiException ex2) { }
            }
        }
    }

    @Override
    protected void _performPostgameTasks() {
        // Do nothing.
    }

}
