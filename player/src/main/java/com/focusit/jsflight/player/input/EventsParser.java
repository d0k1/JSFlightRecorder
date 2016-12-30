package com.focusit.jsflight.player.input;

import com.focusit.jsflight.player.constants.EventConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sorted list of recorded events
 *
 * @author Denis V. Kirpichenkov
 */
public class EventsParser
{
    public static List<JSONObject> parse(List<String> jsonEncodedListsOfEvents)
    {
        return jsonEncodedListsOfEvents
                .stream()
                .map(EventsParser::parse)
                .flatMap(Collection::stream)
                .sorted(EventsParser::sortEvents)
                .collect(Collectors.toList());
    }

    public static List<JSONObject> parse(String jsonEncodedListOfEvents)
    {
        if (jsonEncodedListOfEvents == null)
        {
            return null;
        }
        List<JSONObject> events = new ArrayList<>();
        JSONArray rawEvents = new JSONArray(jsonEncodedListOfEvents);
        rawEvents.iterator().forEachRemaining(event -> {
            if (!event.toString().contains(EventConstants.FLIGHT_CP))
            {
                events.add(new JSONObject(event));
            }
        });

        Collections.sort(events, EventsParser::sortEvents);
        return events;
    }

    private static int sortEvents(JSONObject o1, JSONObject o2)
    {
        return ((Long)o1.getLong(EventConstants.TIMESTAMP)).compareTo(o2.getLong(EventConstants.TIMESTAMP));
    }
}
