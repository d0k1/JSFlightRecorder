/**
 * Core methods.
 * Init
 * Start Stop
 * Timers
 * 
 */
// jsflight namespace 
var jsflight = jsflight || {};


/**
 * Start recorder
 * 
 * @returns {Boolean}
 */
jsflight.startRecorder = function() {
    // do not forget about timers. first stop them, next start them if required
    jsflight.stopTimers();
    jsflight.startTimers();

    if (document.addEventListener) {
        document.addEventListener('click', jsflight.TrackMouse);
        document.addEventListener('mousedown', jsflight.TrackMouse);
        document.addEventListener('mousemove', jsflight.TrackMouse);
        document.addEventListener('mousewheel', jsflight.TrackMouse);
        document.addEventListener('scroll', jsflight.TrackMouse);
        document.addEventListener('keypress', jsflight.TrackKeyboard);
        document.addEventListener('keyup', jsflight.TrackKeyboard);
        document.addEventListener('keydown', jsflight.TrackKeyboard);
        document.addEventListener('paste', jsflight.TrackKeyboard);
        window.addEventListener('hashchange', jsflight.TrackHash);
    } else {
        document.attachEvent('onclick', jsflight.TrackMouse);
        document.attachEvent('onmousedown', jsflight.TrackMouse);
        document.attachEvent('onmousemove', jsflight.TrackMouse);
        document.attachEvent('onmousewheel', jsflight.TrackMouse);
        document.attachEvent('onscroll', jsflight.TrackMouse);
        document.attachEvent('onkeypress', jsflight.TrackKeyboard);
        document.attachEvent('onkeyup', jsflight.TrackKeyboard);
        document.attachEvent('onkeydown', jsflight.TrackKeyboard);
        document.attachEvent('onpaste', jsflight.TrackKeyboard);
        window.attachEvent('onhashchange', jsflight.TrackHash);
    }
    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return false;
    }

    window.sessionStorage.setItem('recorder.active', 'true');
    jsflight.eventId = +window.sessionStorage.getItem('recorder.max') + 1;
    if (!jsflight.eventId) {
        jsflight.eventId = 0;
    }

    if (jsflight.options.trackXhr) {
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
    if (document.removeEventListener) {
        document.removeEventListener('click', jsflight.TrackMouse);
        document.removeEventListener('mousedown', jsflight.TrackMouse);
        document.removeEventListener('mousemove', jsflight.TrackMouse);
        document.removeEventListener('mousewheel', jsflight.TrackMouse);
        document.removeEventListener('scroll', jsflight.TrackMouse);
        document.removeEventListener('keypress', jsflight.TrackKeyboard);
        document.removeEventListener('keyup', jsflight.TrackKeyboard);
        document.removeEventListener('keydown', jsflight.TrackKeyboard);
        window.removeEventListener('hashchange', jsflight.TrackHash);
    } else {
        document.detachEvent('click', jsflight.TrackMouse);
        document.detachEvent('mousedown', jsflight.TrackMouse);
        document.detachEvent('mousemove', jsflight.TrackMouse);
        document.detachEvent('mousewheel', jsflight.TrackMouse);
        document.detachEvent('scroll', jsflight.TrackMouse);
        document.detachEvent('keypress', jsflight.TrackKeyboard);
        document.detachEvent('keyup', jsflight.TrackKeyboard);
        document.detachEvent('keydown', jsflight.TrackKeyboard);
        window.detachEvent('hashchange', jsflight.TrackHash);
    }

    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return false;
    }

    window.sessionStorage.removeItem('recorder.active');
    window.sessionStorage.removeItem('recorder.max');

    if (jsflight.options.trackXhr) {
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

    return !!window.sessionStorage.getItem('recorder.active');
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