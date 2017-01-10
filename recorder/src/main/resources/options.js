/**
 * Methods for configuration
 */
// jsflight namespace 
var jsflight = jsflight || {};

/* =================================================================================================================================== */
// recorder options
jsflight.options = {
    // should record be started after page loaded
    autostart : false,
    // baseUrl to reach servlet. useful when logger is outside an web-app
    baseUrl : '',
    // url to find servlet to send data
    downloadPath : '/jsflight/recorder/storage',
    // url to find servlet to view status
    statusPath : '/jsflight/recorder/status',
    // save initial dom after document has loaded
    saveInitialDom : false,
    // save page shot when document loaded
    saveInitialScreenshot : false,
    // take screenshot on hash change
    saveShotOnHashChange : false,
    // take DOM snapshot when has changing
    saveDomOnHashChange : false,
    // track mouse movements
    trackMouse : false,
    // track url hash change
    trackHash : false,
    // track xhr request/response
    trackXhr : false,
    // track ping request/response
    trackPing : false,
    // ping request path's substring to identify that certain request is ping request
    pingPathSubstring : "ping_payload.dat",
    // time to track, milliseconds
    track_duration : -1,
    // time interval to send tracked data, milliseconds
    send_interval : -1,
    // control panel disabled by default
    cp_disabled : true,
    // list of element`s attributes to check and store
    attributes_to_store : [],
    //list of ids or id prefixes to ignore
    id_exclusions : [],
    // function that checks if dom ready to be dumped or pictured or not
    dom_ready_check: function(){
        return true;
    },
    propertyProvider : function(prop) {
    },
    scrollHelperFunction: function(element, paths) {
    }

};

/* =================================================================================================================================== */
/*Regex to test id exclusion. Initialized at parseOptions */
jsflight.exclusion_regexp;



/* =================================================================================================================================== */

jsflight.parseOptions = function(options) {
    if (options.saveInitialDom)
        jsflight.options.saveInitialDom = options.saveInitialDom;

    if (options.saveInitialScreenshot)
        jsflight.options.saveInitialScreenshot = options.saveInitialScreenshot;

    if (options.saveShotOnHashChange)
        jsflight.options.saveShotOnHashChange = options.saveShotOnHashChange;

    if (options.saveDomOnHashChange)
        jsflight.options.saveDomOnHashChange = options.saveDomOnHashChange;

    if (options.cp_disabled === false)
        jsflight.options.cp_disabled = false;

    if (options.autostart)
        jsflight.options.autostart = options.autostart;

    if (options.baseUrl)
        jsflight.options.baseUrl = options.baseUrl;

    if (options.downloadPath)
        jsflight.options.downloadPath = options.downloadPath;

    if (options.statusPath)
        jsflight.options.statusPath = options.statusPath;

    if (options.trackMouse)
        jsflight.options.trackMouse = options.trackMouse;

    if (options.trackHash)
        jsflight.options.trackHash = options.trackHash;

    if (options.trackXhr)
        jsflight.options.trackXhr = options.trackXhr;

    if (options.trackPing)
        jsflight.options.trackPing = options.trackPing;

    if (options.propertyProvider)
        jsflight.options.propertyProvider = options.propertyProvider;

    if (options.dom_ready_check)
        jsflight.options.dom_ready_check = options.dom_ready_check;

    if (options.send_interval)
        jsflight.options.send_interval = options.send_interval;

    if (options.track_duration)
        jsflight.options.track_duration = options.track_duration;

    if (options.attributes_to_store)
        jsflight.options.attributes_to_store = options.attributes_to_store;

    if(options.id_exclusions)
        jsflight.options.id_exclusions = options.id_exclusions;
        jsflight.exclusion_regexp = new RegExp(jsflight.options.id_exclusions.join('|'))

    if(options.scrollHelperFunction)
        jsflight.options.scrollHelperFunction = options.scrollHelperFunction
};


