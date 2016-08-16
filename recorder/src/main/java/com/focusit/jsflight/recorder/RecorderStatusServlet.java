package com.focusit.jsflight.recorder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * Servlet to show some status information. Currently unused at all
 *
 * @author Denis V. Kirpichenkov
 */
@WebServlet(urlPatterns = { "/jsflight/recorder/status" })
public class RecorderStatusServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream("status.html"))
        {
            resp.setContentType("text/html;charset=UTF-8");
            try (OutputStream out = resp.getOutputStream())
            {
                IOUtils.copy(is, out);
                out.flush();
            }
        }
    }
}
