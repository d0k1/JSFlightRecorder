/**
 * Event storage method
 * Event send method
 */

 
// jsflight namespace 
var jsflight = jsflight || {};

jsflight.checkTargetDetach = function(element){
    if(!element || !element.parentNode || !element.tagName){
        return false;
    }
    while(element.parentNode != null){
        element = element.parentNode;
    }
    if(!element.tagName){
        return false;
    }
    return !(element.tagName.toLowerCase === 'html')
}

jsflight.getDetachElement = function(event){
    var targetPath = event.path;
    for(var i=0; i< targetPath.length; i++){
        if(!targetPath[i].parentNode){
            return targetPath[i+1];
        }
    }
}

jsflight.getEventInfo = function(event) {
    event = event || window.event;

    event.target = event.target || event.srcElement;

    var result = {};

    var iframeDocument = event.target.ownerDocument;
    var iframeWindow = iframeDocument ? (iframeDocument.defaultView || iframeDocument.parentWindow) : event.target;

    if (iframeWindow.xpath) {
        result.iframeXpath = iframeWindow.xpath;
    }
    result.iframeIndices = iframeWindow.iframeIndices;

    var inputData = jsflight.getInputData(event.target);

    result.caretPosition = inputData.selectionStart;
    result.selectionEnd = inputData.selectionEnd;
    result.isSelection = inputData.isSelection;
    if(!jsflight.isIE()){
        try {
            result.clipboardData = (event.clipboardData || window.clipboardData).getData('Text');
        } catch(e) {}
    }
    result.tabuuid = jsflight.tabUuid;
    result.type = event.type;
    result.url = window.location.href;
    result.which = event.which;
    result.key = event.key;
    result.keyCode = event.keyCode;
    result.charCode = event.charCode;

    if (event.type === 'keyup') {
        if (!event.shiftKey) {
            result.charCode = String.fromCharCode(result.charCode).toLowerCase().charCodeAt(0);
        }
    }
    result.altKey = event.altKey;
    result.ctrlKey = event.ctrlKey;
    result.shiftKey = event.shiftKey;
    result.metaKey = event.metaKey;
    result.button = event.button;
    result.hash = window.location.hash;

    result.target = jsflight.getElementXPath(event.target);
    result.target1 = jsflight.getTargetId(event);
    result.target2 = jsflight.getElementXpathId(event.target);

    if(!result.target2 && jsflight.checkTargetDetach(event.target)){
        result.target2 = jsflight.getDetachedElementXpathId(event.target, jsflight.getDetachElement(event));
    }

    result.timestamp = Date.now();

    result.screenX = event.screenX;
    result.screenY = event.screenY;

    result.pageX = event.pageX;
    result.pageY = event.pageY;

    result.screen = {
            width : screen.width,
            height : screen.height
            };

    result.window = {
                width : window.outerWidth,
                height : window.outerHeight
            };

    result.page = {
                width : window.innerWidth,
                height : window.innerHeight
            };

    result.agent = navigator.userAgent;
    result.image = event.image;
    result.dom = event.dom;
    result.eventId = event.eventId;

    result.newUrl = event.newURL;
    result.oldUrl = event.oldURL;

    result.deltaX = event.deltaX;
    result.deltaY = event.deltaY;
    result.deltaZ = event.deltaZ;

    result.wheelDelta = event.wheelDelta;

    if (jsflight.options.propertyProvider) {
        jsflight.options.propertyProvider(result);
    }

    return result;
};

/**
 * Get input data:current caret position(stored in selectionStart property),
 * selectionEnd(if no selection equals to selectionStart) and presence of a selection in input
 */
jsflight.getInputData = function (node) {
    var start = node && 'selectionStart' in node ? node.selectionStart :0 ;
    var end = node && 'selectionEnd' in node ? node.selectionEnd : 0;
    return {selectionStart: start, selectionEnd:end, isSelection: start != end}
 }

/**
 * Store event to session storage
 * 
 * @param eventid
 * @param eventdata
 */
jsflight.saveToStorage = function(eventid, eventdata) {
    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return;
    }

    window.sessionStorage.setItem("recorder.max", eventid);
    window.sessionStorage.setItem('recorder.eventId.' + eventid, eventdata);
};

/**
 * Clear session storage
 */
jsflight.clearStorage = function() {
    jsflight.eventId = 0;
    sessionStorage.clear();
};

/**
 * send current batch of stored events and clear session storage
 */
jsflight.sendEventData = function(sendStop) {

    if (jsflight.started === false)
        return;

    if (typeof (window.sessionStorage) == "undefined") {
        console.log('No support of window.sessionStorage');
        return;
    }
    var storage = window.sessionStorage;

    var events = [];
    var keys = [];
    for ( var key in storage) {
        if (key.indexOf('recorder.eventId.') === 0) {
            events.push(storage.getItem(key));
            keys.push(key);
        }
    }

    // it is pity to send an empty array. No tracked data no xhr post
    if (events.length === 0 && sendStop === false)
        return;

    var data = JSON.stringify(events);
    var uri = jsflight.options.baseUrl + jsflight.options.downloadPath;
    if (sendStop) {
        uri += "?stop";
    }
    var xhr = new XMLHttpRequest();
    xhr.open('POST', uri, true);
    xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xhr.onload = function() {
        if (xhr.status == 200) {
            for (var i = 0; i < keys.length; i++) {
                storage.removeItem(keys[i]);
            }
        } else {
            console.log("error storing data. status " + xhr.status);
        }
    };
    xhr.send('data=' + encodeURIComponent(data));
};

jsflight.isIE = function(){
    var ua = window.navigator.userAgent;

        var msie = ua.indexOf('MSIE ');
        var trident = ua.indexOf('Trident/');
        var edge = ua.indexOf('Edge/');
        if (msie > 0 || trident > 0 || edge > 0) {
            return true
        }
        return false;
}
