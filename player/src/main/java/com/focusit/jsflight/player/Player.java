package com.focusit.jsflight.player;

import com.beust.jcommander.JCommander;
import com.focusit.jsflight.player.cli.CliPlayer;
import com.focusit.jsflight.player.cli.config.CliConfig;
import com.focusit.jsflight.player.cli.config.IConfig;
import com.focusit.jsflight.player.cli.config.PropertiesConfig;
import com.focusit.jsflight.player.ui.ExceptionDialog;
import com.focusit.jsflight.player.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;

public class Player
{
    private static final Logger LOG = LoggerFactory.getLogger(Player.class);

    public static void main(String[] args) throws Exception
    {
        LOG.info("Main entry to program");
        IConfig config = getConfig(args);

        if (config.isHeadlessModeEnabled())
        {
            LOG.info("Starting in headless mode");
            CliPlayer player = new CliPlayer();
            boolean errorOccurred = false;
            try
            {
                player.play(config);
            }
            catch (Exception e)
            {
                LOG.error(e.toString(), e);
                errorOccurred = true;
            }
            System.exit(errorOccurred ? 1 : 0);
        }
        else
        {
            startWithUI();
        }
    }

    private static IConfig getConfig(String[] args) {
        LOG.info("Configs parsing");
        IConfig config;
        if (Player.class.getResource("application.properties") != null)
        {
            config = new PropertiesConfig(Player.class.getResource("application.properties").getPath());
        }
        else if (new File(System.getProperty("configFile")).exists())
        {
            config = new PropertiesConfig(System.getProperty("configFile"));
        }
        else
        {
            config = new CliConfig();
            JCommander jc = new JCommander(config, args);
            jc.setProgramName(Player.class.getSimpleName());
            if (((CliConfig)config).shouldShowUsage())
            {
                jc.usage();
                System.exit(0);
            }
        }
        return config;
    }

    private static void startWithUI()
    {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOG.error(e.toString(), e);
            ExceptionDialog ld = new ExceptionDialog("Error", e.toString(), e);
            ld.setVisible(true);
        });
        EventQueue.invokeLater(() -> {
            try
            {
                MainFrame window = new MainFrame();
                window.getFrame().setVisible(true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }
}
