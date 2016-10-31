package com.focusit.jsflight.player.cli.config;

import com.beust.jcommander.IStringConverter;
import com.focusit.jsflight.player.constants.BrowserType;

public class BrowserTypeConverter implements IStringConverter<BrowserType> {

    @Override
    public BrowserType convert(String value) {
        return BrowserType.valueOf(value);
    }
}
