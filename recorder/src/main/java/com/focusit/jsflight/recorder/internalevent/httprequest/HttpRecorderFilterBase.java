package com.focusit.jsflight.recorder.internalevent.httprequest;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.focusit.jsflight.recorder.internalevent.InternalEventRecorder;

public abstract class HttpRecorderFilterBase implements Filter
{
    public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";
    private static final AtomicBoolean enabled = new AtomicBoolean(false);

    public final static void setEnabled(boolean enabled)
    {
        HttpRecorderFilterBase.enabled.set(enabled);
    }

    @Override
    public final void destroy()
    {
        InternalEventRecorder.getInstance().stopRecording();
        try
        {
            InternalEventRecorder.getInstance().shutdown();
        }
        catch (InterruptedException e)
        {
            logException(e);
        }
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse))
        {
            throw new ServletException("OncePerRequestFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;

        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
        boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

        if (hasAlreadyFilteredAttribute || skipDispatch(httpRequest) || doNotRecordRequest(httpRequest, httpResponse))
        {

            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        }
        else
        {
            // Do invoke this filter...
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            try
            {
                doFilterInternal(httpRequest, httpResponse, filterChain);
            }
            finally
            {
                // Remove the "already filtered" request attribute for this request.
                request.removeAttribute(alreadyFilteredAttributeName);
            }
        }
    }

    public final void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        if (!HttpRecorderFilterBase.enabled.get())
        {
            chain.doFilter(request, response);
            return;
        }

        if (doNotRecordRequest(request, response))
        {
            chain.doFilter(request, response);
            return;
        }

        HttpRecordInformation info = new HttpRecordInformation();
        RecordableHttpServletRequest requestForRecord = null;
        try
        {
            try
            {
                prepareHttpRecordInfo(info, request, response);
                requestForRecord = HttpRecorder.prepareRequestToRecord(request, info);
            }
            catch (Exception e)
            {
                logException(e);
            }
            chain.doFilter(requestForRecord, response);

        }
        finally
        {
            try
            {
                updateHttpRecordInfo(info, requestForRecord, response);
                HttpRecorder.recordRequest(requestForRecord, info);
            }
            catch (InterruptedException e)
            {
                logException(e);
            }
        }
    }

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException
    {
        InternalEventRecorder.getInstance().startRecording();
    }

    public abstract void prepareHttpRecordInfo(HttpRecordInformation info, ServletRequest request,
            ServletResponse response);

    public abstract void updateHttpRecordInfo(HttpRecordInformation info, ServletRequest request,
            ServletResponse response);

    protected abstract boolean doNotRecordRequest(HttpServletRequest request, HttpServletResponse response);

    protected String getAlreadyFilteredAttributeName()
    {
        String name = "HttpRecorder";
        return name + ALREADY_FILTERED_SUFFIX;
    }

    protected abstract void logException(Exception e);

    protected boolean shouldNotFilterAsyncDispatch()
    {
        return false;
    }

    protected boolean shouldNotFilterErrorDispatch()
    {
        return false;
    }

    private boolean skipDispatch(HttpServletRequest request)
    {
        if (shouldNotFilterErrorDispatch())
        {
            return true;
        }
        return false;
    }

}
