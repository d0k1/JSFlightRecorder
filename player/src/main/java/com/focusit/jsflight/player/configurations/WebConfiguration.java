package com.focusit.jsflight.player.configurations;

import org.apache.commons.lang3.StringUtils;

import com.focusit.jsflight.player.configurations.interfaces.IDefaults;

/**
 * Created by dkirpichenkov on 06.05.16.
 */
public class WebConfiguration implements IDefaults
{
    private String placeholders;
    private String selectXpath;

    public String getSelectXpath()
    {
        return selectXpath;
    }

    public void setSelectXpath(String selectXpath)
    {
        this.selectXpath = selectXpath;
    }

    public String getPlaceholders()
    {
        return placeholders;
    }

    public void setPlaceholders(String placeholders)
    {
        this.placeholders = placeholders;
    }

    @Override
    public void loadDefaults()
    {
        if (StringUtils.isBlank(getPlaceholders()))
        {
            setPlaceholders("");
        }
        if (StringUtils.isBlank(getSelectXpath()))
        {
            setSelectXpath(null);
        }
    }
}
