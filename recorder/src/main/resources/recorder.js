/**
 * Core methods.
 * Init
 * Start Stop
 * Timers
 * 
 */
// jsflight namespace 
var jsflight = jsflight || {};

function bind(el, eventType, handler) {
    if (el.addEventListener) { // DOM Level 2 browsers
        el.addEventListener(eventType, handler, false);
    } else if (el.attachEvent) { // IE <= 8
        el.attachEvent('on' + eventType, handler);
    } else { // ancient browsers
        el['on' + eventType] = handler;
    }
}

function unbind(el, eventType, handler) {
    if (el.removeEventListener) {
        el.removeEventListener(eventType, handler, false);
    } else if (el.detachEvent) {
        el.detachEvent("on" + eventType, handler);
    } else {
        el["on" + eventType] = null;
    }
}

addEventListeners = function(window, document) {
    bind(document, 'click', jsflight.TrackMouse);
    bind(document, 'mousedown', jsflight.TrackMouse);
    bind(document, 'mousemove', jsflight.TrackMouse);
    bind(document, 'mousewheel', jsflight.TrackMouse);
    bind(document, 'scroll', jsflight.TrackMouse);
    bind(document, 'keypress', jsflight.TrackKeyboard);
    bind(document, 'keyup', jsflight.TrackKeyboard);
    bind(document, 'keydown', jsflight.TrackKeyboard);
    bind(document, 'paste', jsflight.TrackKeyboard);
    bind(window, 'hashchange', jsflight.TrackHash);
}

removeEventListeners = function(window, document) {
    unbind(document, 'click', jsflight.TrackMouse);
    unbind(document, 'mousedown', jsflight.TrackMouse);
    unbind(document, 'mousemove', jsflight.TrackMouse);
    unbind(document, 'mousewheel', jsflight.TrackMouse);
    unbind(document, 'scroll', jsflight.TrackMouse);
    unbind(document, 'keypress', jsflight.TrackKeyboard);
    unbind(document, 'keyup', jsflight.TrackKeyboard);
    unbind(document, 'keydown', jsflight.TrackKeyboard);
    unbind(window, 'hashchange', jsflight.TrackHash);
}

function getXpathToIFrame(iframe) {
    if (iframe.id) {
        return "//iframe[@id='" + iframe.id + "']";
    }
    if (iframe.name) {
        return "//iframe[@name='" + iframe.name + "']";
    }
    if (iframe.title) {
        return "//iframe[@title='" + iframe.title + "']";
    }
    return Xpath.getElementXPath(iframe);
}

function findFrameLocalIndex(win) {
    win = win || window; // Assume self by default
    if (win.parent != win) {
        for (var i = 0; i < win.parent.frames.length; i++) {
            if (win.parent.frames[i] == win) {
                return i;
            }
        }
        throw Error("In a frame, but could not find myself");
    } else {
        return -1;
    }
}

function findFrameFullIndex(win) {
     win = win || window; // Assume self by default
     if (findFrameLocalIndex(win) < 0) {
         return "";
     } else {
         return findFrameFullIndex(win.parent) + "." + findFrameLocalIndex(win);
     }
}

/**
 * window - window, from which search should start
 * returns the array of windows. Initial window will be returned too
 */
function getDescendantWindows(window) {
    var result = [];
    var queue = [window];
    while (queue.length > 0) {
        var currentWindow = queue.pop();

        var windows = currentWindow.frames;
        for (var i = 0; i < windows.length; i++) {
            var iframeWindow = windows[i];
            iframeWindow.xpath = (currentWindow.xpath ? currentWindow.xpath + "||" : "") +
                getXpathToIFrame(iframeWindow.frameElement);
            iframeWindow.iframeIndices = findFrameFullIndex(iframeWindow).substring(1);
            queue.push(iframeWindow);
        }
        result.push(currentWindow);
    }

    return result;
}

jsflight.bindAllHandlers = function() {
    var windows = getDescendantWindows(window || document.defaultView);
    for (var i = 0; i < windows.length; i++) {
        var win = windows[i];
        addEventListeners(win, win.document);
    }
}

jsflight.unbindAllHandlers = function() {
    var windows = getDescendantWindows(window || document.defaultView);
    for (var i = 0; i < windows.length; i++) {
        var win = windows[i];
        removeEventListeners(win, win.document);
    }
}

jsflight.rebindAllHandlers = function() {
    jsflight.unbindAllHandlers();
    jsflight.bindAllHandlers();
}

/**
 * Start recorder
 * 
 * @returns {Boolean}
 */
jsflight.startRecorder = function() {
    // do not forget about timers. first stop them, next start them if required
    jsflight.stopTimers();
    jsflight.startTimers();

    jsflight.bindAllHandlers();

    if (!window.sessionStorage) {
        console.log('No support of window.sessionStorage');
        return false;
    }

    window.sessionStorage.setItem('recorder.active', 'true');
    jsflight.eventId = +window.sessionStorage.getItem('recorder.max') + 1;
    if (!jsflight.eventId) {
        jsflight.eventId = 0;
    }

    if (jsflight.options.trackXhr || jsflight.options.trackPing) {
        jsflight.initXhrTracking();
    }

    jsflight.started = true;
};

/**
 * Stop recorder
 * 
 * @returns {Boolean}
 */
jsflight.stopRecorder = function() {
    jsflight.stopTimers();

    jsflight.unbindAllHandlers();

    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return false;
    }

    window.sessionStorage.removeItem('recorder.active');
    window.sessionStorage.removeItem('recorder.max');

    if (jsflight.options.trackXhr || jsflight.options.trackPing) {
        jsflight.stopXhrTracking();
    }

    jsflight.started = false;
};

/**
 * Will recording be continued in case of page reload
 * 
 * @returns {Boolean}
 */
jsflight.shouldStartOnLoad = function() {

    if (jsflight.options.autostart === true) {
        return true;
    }

    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return false;
    }

    return Boolean(window.sessionStorage.getItem('recorder.active'));
};

/**
 * Init method. should be called on host page. uses onload and onbeforeunload
 * mechanics to add/remove event listeners
 * 
 * @param trackMouseMove
 */
jsflight.addJSFlightHooksOnDocumentLoad = function(options) {

    jsflight.parseOptions(options);
    

    function initialDump() {
        if (jsflight.options.saveInitialScreenshot) {
            jsflight.take_a_screenshot();
        }

        if (jsflight.options.saveInitialDom) {
            jsflight.take_dom_snapshot();
        }
    }

    // when document is rendered
    window.onload = function() {

        if(jsflight.options.saveInitialScreenshot || jsflight.options.saveInitialDom){
            window.setTimeout(200, initialDump());            
            window.setTimeout(2500, initialDump());
            window.setTimeout(5500, initialDump());
            window.setTimeout(9000, initialDump());
        }

        jsflight.addControlHook();
    };

    // when tab is above to close
    window.onbeforeunload = function() {
        jsflight.stopTimers();
        // disable all event handlers
        jsflight.removeControlHook();
        // send captured events
        jsflight.sendEventData(true);
    };
};

jsflight.startTimers = function() {
    if (jsflight.options.track_duration > 0)
        jsflight.stop_timer = window.setInterval(jsflight.stop_recording_timer,
                jsflight.options.track_duration);

    if (jsflight.options.send_interval > 0)
        jsflight.send_timer = window.setInterval(jsflight.send_data_timer,
                jsflight.options.send_interval);
};

jsflight.stopTimers = function() {
    if (jsflight.stop_timer !== null) {
        window.clearInterval(jsflight.stop_timer);
        jsflight.stop_timer = null;
    }

    if (jsflight.send_timer !== null) {
        window.clearInterval(jsflight.send_timer);
        jsflight.send_timer = null;
    }
};

jsflight.take_a_screenshot = function() {
    var event = {};
    // TODO : run dom_ready_check until it returns true, so dom is ready to be pictured
    html2canvas(document.body, {
        onrendered : function(canvas) {
            try {
                event.image = canvas.toDataURL("image/png");
                event.type = 'screenshot';
                event.timeStamp = Date.now();
                event.eventId = jsflight.eventId;
                var data = JSON.stringify(jsflight.getEventInfo(event));
                jsflight.saveToStorage(jsflight.eventId, data);
            } catch (e) {
                console.log(e);
            } finally {
                jsflight.eventId++;
            }
        }
    });
};

jsflight.take_dom_snapshot = function() {
    // TODO : run dom_ready_check until it returns true, so dom is ready to be dumped
    try {
        var tree = Xml.getElementHTML(document.body);
        var event = {};
        event.type = 'snapshot';
        event.timeStamp = Date.now();
        event.dom = tree;
        event.eventId = jsflight.eventId;
        var data = JSON.stringify(jsflight.getEventInfo(event));
        jsflight.saveToStorage(jsflight.eventId, data);
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
};

jsflight.stop_recording_timer = function() {
    jsflight.sendEventData(true);
    jsflight.stopRecorder();
};

jsflight.send_data_timer = function() {
    jsflight.sendEventData(false);
};

(function (window, document) {
    var MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
    var eventListenerSupported = window.addEventListener;

    if (MutationObserver) {
        window.obs = new MutationObserver(function (mutations, observer) {
            jsflight.rebindAllHandlers();
        });

        window.obs.observe(document, {childList:true, subtree:true});
    }
    else if (eventListenerSupported) {
        document.addEventListener('DOMNodeInserted', function (event) {
            jsflight.rebindAllHandlers();
        }, false);
    }
})(window, document);