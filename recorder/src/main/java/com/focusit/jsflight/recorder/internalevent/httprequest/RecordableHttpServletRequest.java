package com.focusit.jsflight.recorder.internalevent.httprequest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RecordableHttpServletRequest extends HttpServletRequestWrapper
{
    private final ByteArrayOutputStream payload;
    private ServletInputStream inputStream;
    private BufferedReader reader;

    public RecordableHttpServletRequest(HttpServletRequest request)
    {
        super(request);
        int contentLength = request.getContentLength();
        this.payload = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        if (this.inputStream == null)
        {
            this.inputStream = new RecordableRequestInputStream(getRequest().getInputStream());
        }
        return this.inputStream;
    }

    public byte[] getPayloadBytes()
    {
        return payload.toByteArray();
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        if (this.reader == null)
        {
            this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }
        return this.reader;
    }

    private class RecordableRequestInputStream extends ServletInputStream
    {

        private final ServletInputStream is;

        public RecordableRequestInputStream(ServletInputStream is)
        {
            this.is = is;
        }

        @Override
        public boolean isFinished()
        {
            return is.isFinished();
        }

        @Override
        public boolean isReady()
        {
            return is.isReady();
        }

        @Override
        public int read() throws IOException
        {
            int ch = this.is.read();
            if (ch != -1)
            {
                payload.write(ch);
            }
            return ch;
        }

        @Override
        public void setReadListener(ReadListener readListener)
        {
            is.setReadListener(readListener);
        }
    }
}
