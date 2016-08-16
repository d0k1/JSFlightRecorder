package com.focusit.jsflight.player;

import java.awt.*;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.jorphan.logging.LoggingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.focusit.jsflight.player.cli.CliConfig;
import com.focusit.jsflight.player.cli.CliPlayer;
import com.focusit.jsflight.player.ui.ExceptionDialog;
import com.focusit.jsflight.player.ui.MainFrame;

public class Player
{
    private static final Logger log = LoggerFactory.getLogger(Player.class);

    public static void main(String[] args) throws Exception
    {
        CliConfig config = new CliConfig();
        JCommander jc = new JCommander(config, args);
        jc.setProgramName(Player.class.getSimpleName());
        if (config.showHelp())
        {
            jc.usage();
            System.exit(0);
        }

        LoggingManager.setPriority(config.getJMeterLogLevel(), "jmeter");
        LoggingManager.setPriority(config.getJMeterLogLevel(), "jorphan");

        if (config.isHeadLess())
        {
            CliPlayer player = new CliPlayer(config);
            try
            {
                player.play();
            }
            catch (Exception e)
            {
                log.error(e.toString(), e);
                System.exit(1);
            }
            finally
            {
                player.getSeleniumDriver().closeWebDrivers();
                System.exit(0);
            }
        }
        else
        {
            startWithUI();
        }
    }

    private static void startWithUI()
    {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
        {

            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                log.error(e.toString(), e);
                ExceptionDialog ld = new ExceptionDialog("Error", e.toString(), e);
                ld.setVisible(true);
            }
        });
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    MainFrame window = new MainFrame();
                    window.getFrame().setVisible(true);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }
}
