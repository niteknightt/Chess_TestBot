package niteknightt.chess.testbot;

import niteknightt.chess.common.AppLogger;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.Helpers;
import niteknightt.chess.common.Settings;

import java.util.Scanner;

public class BotMain {

    public static void main(String[] args) throws Exception {
        //Helpers.initLog();
        Settings.createInstance(Enums.SettingsType.BOTTERBOT);
        AppLogger.createInstance(Enums.SettingsType.BOTTERBOT, Enums.LogLevel.DEBUG, true);
        BotManager botManager = new BotManager();
        Thread botManagerThread = new Thread(botManager);
        botManagerThread.start();

        boolean gotQuitCommand = false;
        while (!gotQuitCommand) {
            System.out.println("Enter quit to quit: ");
            Scanner sc= new Scanner(System.in);
            String str= sc.nextLine();
            if ("quit".equalsIgnoreCase(str)) {
                gotQuitCommand = true;
            }
            sc.close();
        }
        botManager.setDone();
        botManagerThread.join();

        System.out.println("Thanks. That is all.");
    }

}
