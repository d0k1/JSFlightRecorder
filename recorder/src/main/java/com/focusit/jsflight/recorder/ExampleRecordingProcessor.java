package com.focusit.jsflight.recorder;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A custom logic example to serve tracked data
 *
 * @author Denis V. Kirpichenkov
 */
public class ExampleRecordingProcessor implements RecordingProcessor
{

    @Override
    public void processDownloadRequest(HttpServletRequest req, HttpServletResponse resp, String result)
            throws IOException
    {
        byte[] data = result.getBytes();
        String name = "record_" + System.currentTimeMillis() + ".json";
        resp.setContentType("application/json");
        resp.setHeader("Content-Transfer-Encoding", "binary");
        resp.setHeader("Content-Length", "" + data.length);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
        resp.getWriter().println(result);
        resp.getWriter().flush();
    }

    @Override
    public void processRecordStop(HttpServletRequest req, HttpServletResponse resp, String data) throws IOException
    {
    }

    @Override
    public void processStoreEvent(HttpServletRequest req, HttpServletResponse resp, String jsonData) throws IOException
    {
        System.err.println(jsonData);
        resp.getWriter().print("{\"OK\"}");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
