/**
 * Event storage method
 * Event send method
 */

(function (window, document, screen, undefined) {
    // jsflight namespace 
    var jsflight = window.jsflight || {};
    window.jsflight = jsflight;

    function isIE(){
        var ua = window.navigator.userAgent;

        var msie = ua.indexOf('MSIE ');
        var trident = ua.indexOf('Trident/');
        var edge = ua.indexOf('Edge/');
        if (msie > 0 || trident > 0 || edge > 0) {
            return true
        }
        return false;
    }

    var objectAssign = Object.assign || function(target) {
        if (target === undefined || target === null) {
            throw new TypeError('Cannot convert first argument to object');
        }

        var to = Object(target);
        for (var i = 1; i < arguments.length; i++) {
            var nextSource = arguments[i];
            if (nextSource === undefined || nextSource === null) {
                continue;
            }
            nextSource = Object(nextSource);

            var keysArray = Object.keys(Object(nextSource));
            for (var nextIndex = 0, len = keysArray.length; nextIndex < len; nextIndex++) {
                var nextKey = keysArray[nextIndex];
                var desc = Object.getOwnPropertyDescriptor(nextSource, nextKey);
                if (desc !== undefined && desc.enumerable) {
                    to[nextKey] = nextSource[nextKey];
                }
            }
        }
        return to;
    };

    function checkTargetDetach(element){
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

    function getDetachElement(event){
        var targetPath = event.path;
        for(var i=0; i< targetPath.length; i++){
            if(!targetPath[i].parentNode){
                return targetPath[i+1];
            }
        }
    }

    // event.type должен быть keypress
    function getChar(event) {
        if (event.which == null) { // IE
            if (event.keyCode < 32) {
                return null; // спец. символ
            }
            return String.fromCharCode(event.keyCode)
        }

        if (event.which != 0 && event.charCode != 0) { // все кроме IE
            if (event.which < 32) return null; // спец. символ
            return String.fromCharCode(event.which); // остальные
        }

        return null; // спец. символ
    }

    function getFrameProperties(event) {
        var result = {};

        var frameDocument = event.target.ownerDocument;
        var frameWindow = frameDocument ? (frameDocument.defaultView || frameDocument.parentWindow) : event.target;

        result.iframeXpath = frameWindow.xpath;
        result.iframeIndices = frameWindow.iframeIndices;

        return result;
    }

    function getEnvironmentDimensions(event) {
        var result = {};

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

        return result;
    }

    function getKeyboardData(event) {
        var result = {};

        if (event.type === 'keypress') {
            result.char = getChar(event);
        }

        result.keyCode = event.keyCode;
        result.charCode = event.charCode;

        result.altKey = event.altKey;
        result.ctrlKey = event.ctrlKey;
        result.shiftKey = event.shiftKey;
        result.metaKey = event.metaKey;

        return result;
    }

    function getMouseData(event) {
        var result = {};

        result.deltaX = event.deltaX;
        result.deltaY = event.deltaY;
        result.deltaZ = event.deltaZ;

        result.wheelDelta = event.wheelDelta;

        result.button = event.button;

        return result;
    }

    jsflight.getEventInfo = function(event) {
        event = event || window.event || {};
        event.target = event.target || event.srcElement || {};

        var result = {};

        objectAssign(result, getFrameProperties(event));
        objectAssign(result, getEnvironmentDimensions(event));
        objectAssign(result, getKeyboardData(event));
        objectAssign(result, getMouseData(event));

        var inputData = getInputData(event.target);

        result.caretPosition = inputData.selectionStart;
        result.selectionEnd = inputData.selectionEnd;
        result.isSelection = inputData.isSelection;

        // ClipboardEvent
        if(!isIE()){
            try {
                result.clipboardData = (event.clipboardData || window.clipboardData).getData('Text');
            } catch(e) {}
        }
        result.tabuuid = jsflight.tabUuid;
        result.type = event.type;
        result.url = window.location.href;

        result.hash = window.location.hash;

        result.target = jsflight.getElementXPath(event.target);
        result.target1 = jsflight.getTargetId(event);
        result.target2 = jsflight.getElementXpathId(event.target);

        if(!result.target2 && checkTargetDetach(event.target)){
            result.target2 = jsflight.getDetachedElementXpathId(event.target, getDetachElement(event));
        }

        result.timestamp = Date.now();

        result.screenX = event.screenX;
        result.screenY = event.screenY;

        result.pageX = event.pageX;
        result.pageY = event.pageY;

        result.agent = navigator.userAgent;
        result.image = event.image;
        result.dom = event.dom;
        result.eventId = event.eventId;

        // HashChangeEvent
        result.newUrl = event.newURL;
        result.oldUrl = event.oldURL;

        if (jsflight.options.propertyProvider) {
            jsflight.options.propertyProvider(result);
        }

        return result;
    };

    /**
     * Get input data:current caret position(stored in selectionStart property),
     * selectionEnd(if no selection equals to selectionStart) and presence of a selection in input
     */
    function getInputData(node) {
        var start = node && 'selectionStart' in node ? node.selectionStart :0 ;
        var end = node && 'selectionEnd' in node ? node.selectionEnd : 0;
        return {selectionStart: start, selectionEnd:end, isSelection: start != end}
     }

    /**
     * Store event to session storage
     * 
     * @param eventId
     * @param eventdata
     */
    jsflight.saveToStorage = function(eventId, eventdata) {
        if (!window.sessionStorage) {
            console.log('No support of window.sessionStorage');
            return;
        }

        window.sessionStorage.setItem("recorder.max", eventId);
        window.sessionStorage.setItem('recorder.eventId.' + eventId, eventdata);
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
        if (!jsflight.started)
            return;

        if (!window.sessionStorage) {
            console.log('No support of window.sessionStorage');
            return;
        }
        var storage = window.sessionStorage;

        var events = [];
        var keys = [];
        for (var key in storage) {
            if (key.startsWith('recorder.eventId.')) {
                events.push(storage.getItem(key));
                keys.push(key);
            }
        }

        // it is pity to send an empty array. No tracked data no xhr post
        if (!events.length && !sendStop)
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
})(window, document, screen);