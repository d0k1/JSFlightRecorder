package com.focusit.jsflight.recorder;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * Servlet provides a js to track events in a browser
 *
 * @author Denis V. Kirpichenkov
 */
@WebServlet(urlPatterns = { "/jsflight/recorder", "/jsflight/recorder.min.js.map" })
public class RecorderServlet extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String filename = req.getParameter("int");

        if (filename == null)
        {
            filename = "recorder.js";
        }

        if (filename.trim().length() == 0 || !filename.toLowerCase().trim().endsWith(".js"))
        {
            filename = "recorder.js";
        }

        if (req.getParameter("min") != null)
        {
            filename = "recorder.min.js";
        }

        if (req.getRequestURI().endsWith("recorder.min.js.map"))
        {
            filename = "recorder.min.js.map";
        }

        String ext = req.getParameter("ext");
        try (InputStream is = ext == null ? getStreamFromResource(filename) : getStreamFromExternalFile(ext))
        {
            if (is == null)
            {
                resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                return;
            }
            resp.setContentType("application/javascript");
            try (OutputStream out = resp.getOutputStream())
            {
                IOUtils.copy(is, out);
                out.flush();
            }
        }
    }

    private InputStream getStreamFromExternalFile(String filename) throws FileNotFoundException
    {
        if (filename.toLowerCase().endsWith(".js"))
        {
            return new FileInputStream(getServletContext().getRealPath(filename));
        }
        return null;
    }

    private InputStream getStreamFromResource(String filename)
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    }
}
