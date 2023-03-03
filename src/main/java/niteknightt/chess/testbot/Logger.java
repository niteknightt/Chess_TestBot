package niteknightt.chess.testbot;

import niteknightt.chess.common.Enums;

public class Logger {
    public static Enums.LogLevel logLevel = Enums.LogLevel.INFO;

    public static void debug(String text) {
        if (logLevel.ordinal() >= Enums.LogLevel.DEBUG.ordinal()) {
            System.out.println("DEBUG:" + text);
        }
    }

    public static void info(String text) {
        if (logLevel.ordinal() >= Enums.LogLevel.INFO.ordinal()) {
            System.out.println("INFO: " + text);
        }
    }

    public static void warning(String text) {
        if (logLevel.ordinal() >= Enums.LogLevel.WARNING.ordinal()) {
            System.out.println("WARNING: " + text);
        }
    }

    public static void error(String text) {
        if (logLevel.ordinal() >= Enums.LogLevel.ERROR.ordinal()) {
            System.out.println("ERROR: " + text);
        }
    }
}
