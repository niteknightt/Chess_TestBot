package niteknightt.chess.testbot;

import niteknightt.chess.common.Enums;

public class EvaluatedMove {
    public String uci;
    public double eval;
    public boolean ismate;
    public int matein;
    public String[] continuation;
    public Enums.MoveEvalCategory evalCategory;
}
