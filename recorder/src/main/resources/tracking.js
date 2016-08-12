/**
 * Tracking methods here
 */

// jsflight namespace 
var jsflight = jsflight || {};

/**
 * Process mouse event
 */
jsflight.TrackMouse = function(mouseEvent) {
    if (mouseEvent.type == 'mousemove' && !jsflight.options.trackMouse) {
        return;
    }

    try {
        if (mouseEvent.target && mouseEvent.target.id) {
            // ignoring self buttons
            if (mouseEvent.target.id.indexOf("no-track-flight-cp") === 0) {
                return;
            }
        }
        mouseEvent.eventId = jsflight.eventId;
        var data = JSON.stringify(jsflight.getEventInfo(mouseEvent));
        jsflight.saveToStorage(jsflight.eventId, data);
    } catch (e) {
        console.log(e);
    } finally {
        // console.log("Event: " + data + "\n");
        jsflight.eventId++;
    }
};

/**
 * Process keyboard event
 */
jsflight.TrackKeyboard = function(keyboardEvent) {
    // ignoring self activation/deactivation
    if (event.ctrlKey && event.altKey && event.shiftKey && (event.which || event.keyCode) == 38) {
        return;
    }
    if (event.ctrlKey && event.altKey && event.shiftKey && (event.which || event.keyCode) == 40) {
        return;
    }
    try {
        if (keyboardEvent.target && keyboardEvent.target.id) {
            // ignoring self buttons
            if (keyboardEvent.target.id.indexOf("no-track-flight-cp") === 0) {
                return;
            }
        }

        keyboardEvent.eventId = jsflight.eventId;
        var data = JSON.stringify(jsflight.getEventInfo(keyboardEvent));
        jsflight.saveToStorage(jsflight.eventId, data);
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
};

jsflight.TrackHash = function(event) {
    if (!jsflight.options.trackHash)
        return;

    try {
        event.eventId = jsflight.eventId;
        event.timeStamp = Date.now();
        var data = JSON.stringify(jsflight.getEventInfo(event));
        jsflight.saveToStorage(jsflight.eventId, data);
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
    
    if(jsflight.options.saveShotOnHashChange){
        jsflight.take_a_screenshot();
    }
    
    if(jsflight.options.saveDomOnHashChange){
        jsflight.take_dom_snapshot();
    }
};

jsflight.TrackXhrOpen = function(data) {
    try {
        data.type = "xhr";
        data.call = "open";
        data.tabuuid = jsflight.tabUuid;
        data.url = window.location.href;
        data.timestamp = new Date().getTime();
        data.eventId = jsflight.eventId;
        data.agent = navigator.userAgent;

        if (jsflight.options.propertyProvider) {
            jsflight.options.propertyProvider(data);
        }

        jsflight.saveToStorage(jsflight.eventId, JSON.stringify(data));
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
};

jsflight.TrackXhrSend = function(data) {
    try {

        if (data.data === null || data.data === undefined) {
            data.data = "";
        }

        var senddata = {
            method : data.open.method,
            target : data.open.target,
            async : data.open.async,
            user : data.open.user,
            password : data.open.password,
            request : data.data,
            xhrId : data.open.xhrId,
            sended : data.data.length
        };
        senddata.type = "xhr";
        senddata.call = "send";
        senddata.tabuuid = jsflight.tabUuid;
        senddata.url = window.location.href;
        senddata.timestamp = new Date().getTime();
        senddata.eventId = jsflight.eventId;
        senddata.agent = navigator.userAgent;

        if (jsflight.options.propertyProvider) {
            jsflight.options.propertyProvider(senddata);
        }

        jsflight.saveToStorage(jsflight.eventId, JSON.stringify(senddata));
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
};

jsflight.TrackXhrStateLoad = function(xhr) {
    if (xhr.currentTarget.openData.target == jsflight.options.baseUrl + jsflight.options.downloadPath) {
        return;
    }
    try {
        var data = {
            method : xhr.currentTarget.openData.method,
            target : xhr.currentTarget.openData.target,
            async : xhr.currentTarget.openData.async,
            user : xhr.currentTarget.openData.user,
            password : xhr.currentTarget.openData.password,
            loaded : xhr.loaded,
            status : xhr.currentTarget.status,
            xhrId : xhr.currentTarget.openData.xhrId,
            response : xhr.currentTarget.response
        };
        data.type = "xhr";
        data.call = "load";
        data.tabuuid = jsflight.tabUuid;
        data.url = window.location.href;
        data.timestamp = new Date().getTime();
        data.eventId = jsflight.eventId;
        data.agent = navigator.userAgent;
        
        if (jsflight.options.propertyProvider) {
            jsflight.options.propertyProvider(data);
        }


        jsflight.saveToStorage(jsflight.eventId, JSON.stringify(data));
    } catch (e) {
        console.log(e);
    } finally {
        jsflight.eventId++;
    }
};

jsflight.initXhrTracking = function() {
    XMLHttpRequest.prototype.oldOpen = XMLHttpRequest.prototype.open;

    XMLHttpRequest.prototype.open = function (method, url, async, user, password) {
        var data = {
            method: method,
            target: url,
            async: async,
            user: user,
            password: password,
            xhrId: jsflight.xhrId
        };
        jsflight.xhrId++;
        // skip open to ourself url
        if (url != jsflight.options.baseUrl + jsflight.options.downloadPath) {
            jsflight.TrackXhrOpen(data);
        }
        this.openData = data;
        this.oldOpen(method, url, async, user, password);
    };

    XMLHttpRequest.prototype.oldSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function(data) {
        if (document.addEventListener) {
            this.addEventListener("load", jsflight.TrackXhrStateLoad, false);
        } else {
            this.attachEvent("load", jsflight.TrackXhrStateLoad, false);
        }
        var trackData = {
            open : this.openData,
            data : data
        };
        // skip send to ourself url
        if (trackData.open.target.indexOf(jsflight.options.baseUrl + jsflight.options.downloadPath) !== 0) {
            jsflight.TrackXhrSend(trackData);
        }
        this.oldSend.call(this, data);
    };
};

jsflight.stopXhrTracking = function() {
    XMLHttpRequest.prototype.open = XMLHttpRequest.prototype.oldOpen;
    XMLHttpRequest.prototype.oldOpen = null;

    XMLHttpRequest.prototype.send = XMLHttpRequest.prototype.oldSend;
    XMLHttpRequest.prototype.oldSend = null;
};

