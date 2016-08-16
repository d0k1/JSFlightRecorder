package com.focusit.jsflight.recorder;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to define custom logic to serve tracked data from a browser
 *
 * @author Denis V. Kirpichenkov
 */
public interface RecordingProcessor
{
    void processDownloadRequest(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException;

    void processRecordStop(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException;

    void processStoreEvent(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException;
}
