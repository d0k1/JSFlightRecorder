package com.focusit.jsflight.server.controllers;

import com.focusit.jsflight.server.model.Experiment;
import com.focusit.jsflight.server.model.Recording;
import com.focusit.jsflight.server.os.OperatingSystemScreenshooter;
import com.focusit.jsflight.server.player.BackgroundWebPlayer;
import com.focusit.jsflight.server.service.RecordingsService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Created by dkirpichenkov on 29.04.16.
 */
@RestController
@RequestMapping(value = "/player")
public class PlayerController
{
    private final static Logger LOG = LoggerFactory.getLogger(PlayerController.class);
    private RecordingsService recordingsService;
    private BackgroundWebPlayer player;

    @Inject
    public PlayerController(RecordingsService recordingsService, BackgroundWebPlayer player)
    {
        this.recordingsService = recordingsService;
        this.player = player;
    }

    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public String getServerVersion() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("git.properties"), writer,
                StandardCharsets.UTF_8);
        return writer.toString();
    }

    /**
     * Get list of uploaded scenarios
     *
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<Recording> getRecordings()
    {
        return recordingsService.getAllRecordings();
    }

    /**
     * Uploading scenario.
     * Parses uploaded file line by line and inserting events to mongodb
     * <p>
     * $ curl -F "name=test.json" -F "file=@./test.json" 127.0.0.1:8080/player/upload
     *
     * @param name
     * @param file
     * @param request
     * @param response
     */
    @RequestMapping(method = RequestMethod.POST, value = "/upload")
    public void uploadScenario(@RequestParam("name") String name, @RequestParam("file") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        if (!recordingsService.importRecording(name, file.getInputStream()))
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get list of current runnable experiments
     * <p>
     * $ curl 127.0.0.1:8080/player/experiments
     */
    @RequestMapping(value = "/experiments", method = RequestMethod.GET)
    public List<Experiment> experiments()
    {
        return player.getAllExperiments();
    }

    /**
     * Play a scenario
     * <p>
     * $ curl "127.0.0.1:8080/player/start?recordingId=572b01abc92e6b697f9d9ab2"
     * $ curl "127.0.0.1:8080/player/start?recordingId=572b01abc92e6b697f9d9ab2&withScreenshots=true&paused=false"
     * $ curl "127.0.0.1:8080/player/start?recordingId=572b01abc92e6b697f9d9ab2&paused=true"
     *
     * @param recordingId id of existing recording
     * @return
     */
    @RequestMapping(value = "/startById", method = RequestMethod.GET)
    public Experiment start(@RequestParam("recordingId") String recordingId,
            @RequestParam(value = "withScreenshots", defaultValue = "false") Boolean withScreenshots,
            @RequestParam(value = "paused", defaultValue = "true") Boolean paused) throws Exception
    {
        return player.start(recordingId, withScreenshots, paused);
    }

    /**
     * Play a scenario
     * <p>
     * $ curl "127.0.0.1:8080/player/start?recordingName=dvic"
     * $ curl "127.0.0.1:8080/player/start?recordingName=farmaimpex&withScreenshots=true&paused=false"
     * $ curl "127.0.0.1:8080/player/start?recordingName=dvic&paused=true"
     *
     * @param recordingName name of existing recording
     * @return Experiment object
     */
    @RequestMapping(value = "/startByName", method = RequestMethod.GET)
    public Experiment start(@RequestParam("recordingName") String recordingName,
            @RequestParam(value = "withScreenshots", defaultValue = "false") Boolean withScreenshots,
            @RequestParam(value = "paused", defaultValue = "true") Boolean paused, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        Optional<Recording> maybeRecording = recordingsService.getAllRecordings().stream()
                .filter(r -> r.getName().equals(recordingName)).findAny();
        for (Recording r : recordingsService.getAllRecordings())
        {
            LOG.debug(r.getName());
        }
        Recording recording = null;
        if (maybeRecording.isPresent())
            recording = maybeRecording.get();
        if (recording == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    String.format("Recording with name '%s' was not found", recordingName));
            return null;
        }
        return start(recording.getId(), withScreenshots, paused);
    }

    /**
     * Get current experiment status: is it played, what is current step
     *
     * @param experimentId
     */
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public Experiment status(@RequestParam("experimentId") String experimentId)
    {
        return player.status(experimentId);
    }

    /**
     * Get screenshot after some step in experiment
     * <p>
     * $ curl "127.0.0.1:8080/player/screenshot?experimentId=573b3169c92e9527bc805cc6&step=0"
     *
     * @param experimentId
     * @param step
     */
    @RequestMapping(value = "/screenshot", method = RequestMethod.GET)
    public void screenshot(@RequestParam("experimentId") String experimentId, @RequestParam("step") Integer step,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InputStream stream = player.getScreenshot(experimentId, step);
        if (stream == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\"", experimentId + String.format("_%05d.png", step)));
        IOUtils.copy(stream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Get screenshot of error which occured during a step of the experiment
     *
     * @param experimentId
     * @param step
     * @throws IOException
     */
    @RequestMapping(value = "/errorScreenShot", method = RequestMethod.GET)
    public void errorScreenshot(@RequestParam("experimentId") String experimentId, @RequestParam("step") Integer step,
            HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InputStream stream = player.getErrorScreenshot(experimentId, step);
        if (stream == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                experimentId + "_error_" + String.format("_%05d.png", step)));
        IOUtils.copy(stream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Download recorded JMeter scenario
     * <p>
     * $ curl "127.0.0.1:8080/player/jmx?experimentId=573b3169c92e9527bc805cc6&index=2"
     *
     * @param experimentId
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/countRecordings", method = RequestMethod.GET)
    public void getCountOfRecordings(@RequestParam("experimentId") String experimentId,
                       HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        int countOfGeneratedScenarios = player.getCountOfGeneratedScenarios(experimentId);

        response.setContentType("text/plain");
        response.getWriter().print(countOfGeneratedScenarios);
        response.flushBuffer();
    }

    /**
     * Download recorded JMeter scenario
     * <p>
     * $ curl "127.0.0.1:8080/player/jmx?experimentId=573b3169c92e9527bc805cc6&index=2"
     *
     * @param experimentId
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping(value = "/jmx", method = RequestMethod.GET)
    public void jmeter(@RequestParam("experimentId") String experimentId, @RequestParam("index") int index,
                       HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        InputStream stream = player.getGeneratedScenario(experimentId, index);
        if (stream == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("text/xml");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", experimentId + ".jmx"));
        IOUtils.copy(stream, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * Skip some steps or go backward in experiment
     * <p>
     * $ curl "127.0.0.1:8080/player/move?experimentId=573b3169c92e9527bc805cc6&step=0"
     *
     * @param experimentId
     * @param step
     */
    @RequestMapping(value = "/move", method = RequestMethod.GET)
    public void move(@RequestParam("experimentId") String experimentId, @RequestParam("step") Integer step,
            @RequestParam(value = "reset", required = false) Boolean reset)
    {
        if (Boolean.TRUE.equals(reset))
        {
            player.move(experimentId, step, true);
        }
        else
            player.move(experimentId, step, false);

    }

    /**
     * Pause playing the scenario
     *
     * @param experimentId
     */
    @RequestMapping(value = "/pause", method = RequestMethod.GET)
    public void pause(@RequestParam("experimentId") String experimentId)
    {
        player.pause(experimentId);
    }

    /**
     * Continue playing the scenario
     *
     * @param experimentId
     */
    @RequestMapping(value = "/resume", method = RequestMethod.GET)
    public void resume(@RequestParam("experimentId") String experimentId) throws Exception
    {
        player.resume(experimentId);
    }

    /**
     * Stop playing scenario at all
     *
     * @param experimentId
     */
    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public void cancel(@RequestParam("experimentId") String experimentId)
    {
        player.cancel(experimentId);
    }

    /**
     * Check if the App can be terminated without any troubles.
     * Checks all opened browsers and determine if them can be freely closed
     *
     * @param experimentId
     */
    @RequestMapping(value = "/terminable", method = RequestMethod.GET)
    public void terminable(@RequestParam("experimentId") String experimentId)
    {

    }

    @RequestMapping(method = RequestMethod.POST, value = "/configure")
    public void configure(@RequestParam("experimentId") String experimentId, HttpServletRequest request)
    {

    }

    /**
     * Get screenshot from specified Xvfb display
     * 
     * curl "http://127.0.0.1:8080/player/xvfb?display=10"
     * curl "http://127.0.0.1:8080/player/xvfb?display=:10"
     *
     * @param displayNumber display number
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/xvfb", method = RequestMethod.GET)
    public void takeNativeScreenshot(@RequestParam("display") String displayNumber, HttpServletResponse response)
            throws Exception
    {
        OperatingSystemScreenshooter.takeXvfbScreenshot(displayNumber, response.getOutputStream());
        response.setContentType("image/png");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s.png\"", displayNumber));
        response.flushBuffer();
    }
}
