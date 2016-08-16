package com.focusit.jsflight.player.fileconfigholder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PostProcessFileConfigHolder extends UIFileConfigHolder
{
    private static final long serialVersionUID = 1L;

    private final static PostProcessFileConfigHolder instance = new PostProcessFileConfigHolder();
    private String filename = "";
    private String script;

    private PostProcessFileConfigHolder()
    {
    }

    public static PostProcessFileConfigHolder getInstance()
    {
        return instance;
    }

    public String getScript()
    {
        return script;
    }

    public void setScript(String script)
    {
        this.script = script;
    }

    @Override
    public void load(String file) throws Exception
    {
        ObjectInputStream stream = getInputStream(file);
        script = (String)stream.readObject();
        filename = (String)stream.readObject();
    }

    @Override
    public void store(String file) throws Exception
    {
        ObjectOutputStream stream = getOutputStream(file);
        stream.writeObject(script);
        stream.writeObject(filename);
    }

    @Override
    protected String getControllerDataFilename()
    {
        return "postprocess";
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }
}
