package com.focusit.jsflight.player.input;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import com.focusit.jsflight.player.constants.EventConstants;

/**
 * Sorted list of recorded events
 *
 * @author Denis V. Kirpichenkov
 */
public class Events
{
    private List<JSONObject> events = new ArrayList<>();

    public List<JSONObject> getEvents()
    {
        return events;
    }

    public List<JSONObject> parse(List<String> content)
    {
        JSONArray rawEvents;
        Set<JSONObject> temp = new HashSet<>();
        for (String line : content)
        {
            rawEvents = new JSONArray(line);
            for (int i = 0; i < rawEvents.length(); i++)
            {
                String event = rawEvents.get(i).toString();
                if (!event.contains(EventConstants.FLIGHT_CP))
                {
                    temp.add(new JSONObject(event));
                }
            }
        }
        events.addAll(temp);
        sortEvents();
        return events;
    }

    public List<JSONObject> parse(String content)
    {
        if (content == null)
        {
            return null;
        }

        JSONArray rawevents;
        Set<JSONObject> temp = new HashSet<>();
        rawevents = new JSONArray(content);

        events.clear();

        for (int i = 0; i < rawevents.length(); i++)
        {
            String event = rawevents.get(i).toString();
            if (!event.contains(EventConstants.FLIGHT_CP))
            {
                temp.add(new JSONObject(event));
            }
        }
        events.addAll(temp);
        sortEvents();
        return events;
    }

    private void sortEvents()
    {
        Collections.sort(events, (o1, o2) -> ((Long)o1.getLong(EventConstants.TIMESTAMP)).compareTo(o2
                .getLong(EventConstants.TIMESTAMP)));
    }
}
