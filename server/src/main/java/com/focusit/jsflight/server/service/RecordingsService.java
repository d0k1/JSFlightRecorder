package com.focusit.jsflight.server.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.focusit.jsflight.server.model.Event;
import com.focusit.jsflight.server.model.Recording;
import com.focusit.jsflight.server.repository.EventRepository;
import com.focusit.jsflight.server.repository.RecordingRepository;
import com.google.gson.Gson;

/**
 * Created by doki on 14.05.16.
 */
@Service
public class RecordingsService
{
    private final static Logger LOG = LoggerFactory.getLogger(RecordingsService.class);

    private RecordingRepository recordingRepository;
    private EventRepository eventRepository;

    @Inject
    public RecordingsService(RecordingRepository recordingRepository, EventRepository eventRepository)
    {
        this.recordingRepository = recordingRepository;
        this.eventRepository = eventRepository;
    }

    public boolean importRecording(String name, InputStream stream)
    {
        String array = "";

        Recording rec = recordingRepository.findByName(name);
        if (rec == null)
        {
            rec = new Recording();
            rec.setName(name);
            rec = recordingRepository.save(rec);
        }
        Recording finalRec = rec;
        Gson gson = new Gson();

        List<CompletableFuture> operations = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(stream, "UTF-8"))
        {
            try (BufferedReader buffered = IOUtils.toBufferedReader(reader))
            {
                array = buffered.readLine();

                while (array != null)
                {

                    String finalArray = array;

                    operations.add(CompletableFuture.supplyAsync(() -> {
                        JSONArray events = new JSONArray(finalArray);
                        List<Event> lineEvents = new ArrayList<>(events.length());

                        for (int i = 0; i < events.length(); i++)
                        {
                            JSONObject eventAsJson = new JSONObject(events.get(i).toString());
                            try
                            {
                                Event event = gson.fromJson(eventAsJson.toString(), Event.class);
                                if (event == null)
                                {
                                    LOG.error("Parsed event was null. Event as JSON: {}", eventAsJson.toString(4));
                                    continue;
                                }
                                lineEvents.add(event);
                            }
                            catch (Throwable t)
                            {
                                LOG.error(t.getMessage(), t);
                                throw t;
                            }
                        }

                        lineEvents.forEach(event -> {
                            event.setRecordingId(finalRec.getId());
                            event.setRecordName(finalRec.getName());
                        });

                        eventRepository.save(lineEvents);

                        return true;
                    }).whenCompleteAsync((aBoolean, throwable) -> {
                        if (throwable != null)
                        {
                            LOG.error(throwable.getMessage(), throwable);
                        }
                    }));

                    array = buffered.readLine();
                }
            }
        }
        catch (IOException e)
        {
            LOG.error(e.getMessage(), e);
        }

        final boolean[] result = { true };

        try
        {
            CompletableFuture.allOf(operations.toArray(new CompletableFuture[operations.size()]))
                    .whenComplete((aVoid, throwable) -> {
                        if (throwable != null)
                        {
                            result[0] = false;
                        }
                    }).get();
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
        }

        LOG.info("While exporting {} events was inserted", eventRepository.getAdded());
        return result[0];
    }

    public List<Recording> getAllRecordings()
    {
        List<Recording> result = new ArrayList<>();
        recordingRepository.findAll().forEach(result::add);
        return result;
    }
}
