package com.focusit.jsflight.recorder;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Do nothing recording processor
 *
 * @author Denis V. Kirpichenkov
 */
public class NoOpRecordingProcessor implements RecordingProcessor
{

    @Override
    public void processDownloadRequest(HttpServletRequest req, HttpServletResponse resp, String data)
            throws IOException
    {
    }

    @Override
    public void processRecordStop(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException
    {
    }

    @Override
    public void processStoreEvent(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException
    {
    }
}
