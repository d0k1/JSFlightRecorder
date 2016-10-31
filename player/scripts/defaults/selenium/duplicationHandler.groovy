import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.constants.EventType
import org.json.JSONArray
/**
 * Базовый скрипт определения дублей событий
 * Скрипту доступны след. переменные:
 *      current - текущее событие
 *      previous - прыдущее событие
 *      logger - лог
 */

static def getTarget(def event){
    if (event.has(EventConstants.SECOND_TARGET)) {
        return event.getString(EventConstants.SECOND_TARGET);
    }
    if (!event.has(EventConstants.FIRST_TARGET)) {
        return "";
    }
    JSONArray array = event.getJSONArray(EventConstants.FIRST_TARGET);
    if (array.isNull(0)) {
        return "";
    }

    String target = array.getJSONObject(0).getString("getxp");
    return target;
}

def nonDuplicateEvents = [EventType.KEY_UP, EventType.KEY_DOWN, EventType.KEY_PRESS, EventType.MOUSE_WHEEL];

def duplicates = [EventType.CLICK, EventType.MOUSE_DOWN];
if(current[EventConstants.SECOND_TARGET] == previous[EventConstants.SECOND_TARGET] && current.type in duplicates && previous.type in duplicates) {
    return true;
}

if(current.type == EventType.MOUSE_DOWN && previous.type == EventType.MOUSE_DOWN &&
        current[EventConstants.SECOND_TARGET].contains('aria-posinset') &&
        previous[EventConstants.SECOND_TARGET].contains('aria-posinset')) {
    return false;
}

if(current.type in nonDuplicateEvents && previous.type in nonDuplicateEvents){
    return false;
}

return previous.type == current.type && previous.url == current.url && getTarget(current) == getTarget(previous);
