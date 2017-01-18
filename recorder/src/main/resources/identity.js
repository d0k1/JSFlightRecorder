/**
 * Method to identify elements 
 */
// jsflight namespace 
var jsflight = jsflight || {};

/**
 * Based on firebug method of getting xpath of dom element
 */
jsflight.getElementXPath = function(element) {
    if (element && element.id)
        return '// *[@id="' + element.id + '"]';
    else
        return jsflight.getElementTreeXPath(element);
};

jsflight.handleSingleAttribute = function(attribute, paths, elementAttributes){
    if(elementAttributes.hasOwnProperty(attribute)){
        paths.splice(0,0,"//*[@"+attribute+"='"+ elementAttributes.getNamedItem(attribute).nodeValue + "']");
    }
}

jsflight.handleAttributeGroup = function(attributeGroup, paths, elementAttributes){
    if(attributeGroup.length == 1){
        jsflight.handleSingleAttribute(attributeGroup, paths, elementAttributes);
        return;
    }
    var presentAttributes = [];
    for(var ind = 0; ind < attributeGroup.length; ind++){
        if(elementAttributes.hasOwnProperty(attributeGroup[ind])){
            presentAttributes.splice(0,0, "@"+attributeGroup[ind]+"='" + elementAttributes.getNamedItem(attributeGroup[ind]).nodeValue + "'");
        }
    }
    if(presentAttributes.length != 0){
            paths.splice(0,0, "//*["+ presentAttributes.join(" and ") + "]");
    }
}

jsflight.checkIdIsNotExcluded = function(id){
  return !jsflight.exclusion_regexp.test(id);
};

/**
 * Function handles an element storing xpath elements to paths array
 */
jsflight.handleElement = function(element, paths){
    for (; element && element.nodeType == 1; element = element.parentNode) {
        var attr = element.attributes;
        var idr = element.id;
        var tag = element.tagName.toLowerCase();
        if(jsflight.options.scrollHelperFunction){
            jsflight.options.scrollHelperFunction(element, paths)
        }
        if(!idr && tag == "input"){
            paths.push("//"+tag);    
        }
        if(!idr && tag == 'img'){
            paths.splice(0,0,"//"+tag);
        }
        if(idr && jsflight.checkIdIsNotExcluded(idr)){
            paths.splice(0,0,"//*[@id='"+idr+"']");
        } else {
            var to_store = jsflight.options.attributes_to_store;
            for(i=0;i<to_store.length;i++){
                if(to_store[i] instanceof Array){
                    jsflight.handleAttributeGroup(to_store[i], paths, attr);
                }else {
                    jsflight.handleSingleAttribute(to_store[i], paths, attr);
                }
            }
        }
    }
}

jsflight.getDetachedElementXpathId = function(originalTargetElement, detachPointElement){
    var paths = [];
    jsflight.handleElement(originalTargetElement, paths);
    jsflight.handleElement(detachPointElement, paths);
    return paths.length == 1 ? "" : paths.join("");
}

jsflight.getElementXpathId = function(element){
    var paths = [];
    jsflight.handleElement(element, paths);
    return paths.length == 1 ? "" : paths.join("");
};

jsflight.getElementTreeXPath = function(element) {
    var paths = [];

    // Use nodeName (instead of localName) so namespace prefix is included (if any).
    for (; element && element.nodeType == 1; element = element.parentNode) {
        var index = 0;
        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {
            // Ignore document type declaration.
            if (sibling.nodeType == Node.DOCUMENT_TYPE_NODE)
                continue;

            if (sibling.nodeName == element.nodeName)
                ++index;
        }

        var tagName = element.nodeName.toLowerCase();
        var pathIndex = (index ? "[" + (index + 1) + "]" : "");
        paths.splice(0, 0, tagName + pathIndex);
    }

    return paths.length ? "/" + paths.join("/") : null;
};

jsflight.getTargetId = function(event) {
    var paths = [];
    var target = event.target;
    
    if(target===null || target === undefined)
        return paths;
    
    var elt = target;
    do{
        paths.push(jsflight.getElementFullId(elt));
        elt = elt.parentNode;
    }while(elt!==undefined && elt!==null && elt!=document);
    
    return paths;
};

function isElement(o) {
    return (
        typeof HTMLElement === "object"
            ? o instanceof HTMLElement //DOM2
            : o && typeof o === "object" && o !== null && o.nodeType === 1 && typeof o.nodeName==="string"
    );
}

jsflight.getElementFullId = function(target) {
    if (!target || !isElement(target)) {
        return null;
    }

    return {
        getxp: Xpath.getElementTreeXPath(target),
        gecp: Css.getElementCSSPath(target),
        gecs: Css.getElementCSSSelector(target),
        csg: new CssSelectorGenerator().getAllSelectors(target)
        /* ,
         csg1 : new CssSelectorGenerator(['id', 'class', 'tag', 'nthchild']).getSelector(target),
         csg2 : new CssSelectorGenerator(['class', 'tag', 'nthchild']).getSelector(target),
         csg3 : new CssSelectorGenerator(['tag', 'nthchild']).getSelector(target),
         csg4 : new CssSelectorGenerator(['nthchild']).getSelector(target)
         */
    };
};


/**
 * Generate browser window/tab uuid
 * 
 * @returns {String}
 */
jsflight.guid = function() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000).toString(16)
                .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
};
