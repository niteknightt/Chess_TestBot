package niteknightt.chess.testbot;

import com.google.gson.Gson;
import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Constants;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.Helpers;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class OpponentProperties {
    public String id;
    public Enums.EngineAlgorithm algorithm;

    public static String OPPONENT_PROPS_FILE_NAME = System.getenv(Constants.ENV_VAR_RUNTIME_FILE_PATH)
        + File.separator
        + Constants.PERSISTENCE_SUBDIR
        + File.separator
        + Constants.OPPONENTS_FILENAME;

    public static List<OpponentProperties> _allProps;

    public static List<OpponentProperties> getAllProps() {
        if (_allProps == null) {
            _loadAllProps();
        }
        return _allProps;
    }

    public static OpponentProperties getForOpponent(String opponentId) {
        if (_allProps == null) {
            _loadAllProps();
        }

        for (int i = 0; i < _allProps.size(); ++i) {
            OpponentProperties props = _allProps.get(i);
            if (props.id.equals(opponentId)) {
                return props;
            }
        }
        return null;
    }

    public static void createOrUpdateAlgorithmForOpponent(String opponentId, Enums.EngineAlgorithm algorithm) {
        OpponentProperties props = new OpponentProperties();
        props.id = opponentId;
        props.algorithm = algorithm;
        createOrUpdateProperties(props);
    }

    public static void createOrUpdateProperties(OpponentProperties newprops) {
        for (int i = 0; i < _allProps.size(); ++i) {
            OpponentProperties props = _allProps.get(i);
            if (props.id.equals(newprops.id)) {
                props.algorithm = newprops.algorithm;
                _saveAllProps();
                return;
            }
        }

        _allProps.add(newprops);
        _saveAllProps();
    }

    protected static void _loadAllProps() {
        File file = Paths.get(OPPONENT_PROPS_FILE_NAME).toFile();

        if (!file.exists()) {
            _allProps = new ArrayList<OpponentProperties>();
            _saveAllProps();
            return;
        }

        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(OPPONENT_PROPS_FILE_NAME));
            _allProps = new LinkedList<OpponentProperties>(Arrays.asList(gson.fromJson(reader, OpponentProperties[].class)));
            reader.close();
        }
        catch (IOException e) {
            AppLogger.getInstance().error("Exception while reading " + OPPONENT_PROPS_FILE_NAME + ": " + e.toString());
            _allProps = new ArrayList<OpponentProperties>();
            _saveAllProps();
        }
    }

    protected static void _saveAllProps() {
        Gson gson = new Gson();
        try {
            Writer writer = Files.newBufferedWriter(Paths.get(OPPONENT_PROPS_FILE_NAME));
            gson.toJson(_allProps, writer);
            writer.close();
        }
        catch (IOException e) {
            AppLogger.getInstance().error("Exception while writing " + OPPONENT_PROPS_FILE_NAME + ": " + e.toString());
        }
    }
}
