package com.focusit.jsflight.player.fileconfigholder;

/**
 * UI Controller interface.
 * Any fileconfigholder must have two main methods to store and load content from a file in certain directory
 *
 * @author dkirpichenkov
 */
public interface IFileConfigHolder
{
    final String defaultConfig = "params";

    /**
     * load controllers data
     *
     * @param file path to directory to store data
     */
    void load(String file) throws Exception;

    /**
     * store fileconfigholder data
     *
     * @param file path to directory to store data
     */
    void store(String file) throws Exception;
}
