package com.focusit.jsflight.recorder;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * Servlet to process tracked data from a browser.
 * It's logic can be overrided by {@link RecorderProcessor}
 *
 * @author Denis V. Kirpichenkov
 */
@WebServlet(urlPatterns = { "/jsflight/recorder/storage" })
public class RecorderStorageServlet extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    private static volatile RecordingProcessor processor = new ExampleRecordingProcessor();

    public static void setProcessor(RecordingProcessor processor)
    {
        RecorderStorageServlet.processor = processor;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        StringWriter writer = new StringWriter();
        IOUtils.copy(req.getInputStream(), writer, "UTF-8");
        String theString = writer.toString();

        String result = java.net.URLDecoder.decode(theString, "UTF-8");
        if (result.length() < 5)
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        RecordingProcessor recProcess = processor;
        if (recProcess == null)
        {
            resp.getWriter().print("{\"OK\"}");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 'cause form's data starts with 'data='
        result = result.substring(5, result.length());
        if (req.getParameter("download") != null)
        {
            recProcess.processDownloadRequest(req, resp, result);
        }
        else if (req.getParameter("stop") != null)
        {
            recProcess.processRecordStop(req, resp, result);
        }
        else
        {
            recProcess.processStoreEvent(req, resp, result);
        }
    }
}
