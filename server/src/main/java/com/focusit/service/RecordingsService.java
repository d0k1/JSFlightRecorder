package com.focusit.service;

import java.io.*;
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

import com.focusit.model.Event;
import com.focusit.model.Recording;
import com.focusit.repository.EventRepositoryCustom;
import com.focusit.repository.RecordingRepository;
import com.google.gson.Gson;

/**
 * Created by doki on 14.05.16.
 */
@Service
public class RecordingsService
{
    private final static Logger LOG = LoggerFactory.getLogger(RecordingsService.class);

    private RecordingRepository recordingRepository;
    private EventRepositoryCustom eventRepository;

    @Inject
    public RecordingsService(RecordingRepository recordingRepository, EventRepositoryCustom eventRepository)
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
                        List<Event> lineEvents = null;
                        JSONArray events = new JSONArray(finalArray);
                        lineEvents = new ArrayList<>(events.length());

                        for (int i = 0; i < events.length(); i++)
                        {
                            JSONObject event = new JSONObject(events.get(i).toString());
                            try
                            {
                                Event e = gson.fromJson(event.toString(), Event.class);
                                if (e == null)
                                {
                                    LOG.error("e==null.\n", event.toString(4));
                                    continue;
                                }
                                lineEvents.add(e);
                            }
                            catch (Throwable t)
                            {
                                LOG.error(t.toString(), t);
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
                            LOG.error(throwable.toString(), throwable);
                        }
                    }));

                    array = buffered.readLine();
                }
            }
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        final boolean[] result = { true };
        CompletableFuture.allOf(operations.toArray(new CompletableFuture[operations.size()])).whenComplete(
                (aVoid, throwable) -> {
                    if (throwable != null)
                    {
                        result[0] = false;
                    }
                });

        return result[0];
    }

    public List<Recording> getAllRecordings()
    {
        ArrayList result = new ArrayList();
        recordingRepository.findAll().forEach(result::add);
        return result;
    }
}
