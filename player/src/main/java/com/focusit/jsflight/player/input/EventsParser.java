package com.focusit.jsflight.player.input;

import com.focusit.jsflight.player.constants.EventConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Sorted list of recorded events
 *
 * @author Denis V. Kirpichenkov
 */
public class EventsParser
{
    public static List<JSONObject> parse(List<String> content)
    {
        List<JSONObject> events = new ArrayList<>();
        for (String line : content)
        {
            events.addAll(parse(line));
        }
        sortEvents(events);
        return events;
    }

    public static List<JSONObject> parse(String content)
    {
        if (content == null)
        {
            return null;
        }
        List<JSONObject> events = new ArrayList<>();
        JSONArray rawEvents = new JSONArray(content);

        for (int i = 0; i < rawEvents.length(); i++)
        {
            String event = rawEvents.get(i).toString();
            if (!event.contains(EventConstants.FLIGHT_CP))
            {
                events.add(new JSONObject(event));
            }
        }
        sortEvents(events);
        return events;
    }

    private static void sortEvents(List<JSONObject> events)
    {
        Collections.sort(events, (o1, o2) -> ((Long)o1.getLong(EventConstants.TIMESTAMP)).compareTo(o2
                .getLong(EventConstants.TIMESTAMP)));
    }
}
