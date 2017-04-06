var helper;
var window;

function HiveHelper(i2b2Window) {
    window = i2b2Window;
}

HiveHelper.prototype.parseXml = parseXml;
HiveHelper.prototype.xPath = xPath;
HiveHelper.prototype.getXNodeVal = getXNodeVal;


/**
 * 
 */
function parseXml(xmlString) {
    var domParser = new window.DOMParser();
    var xmlDoc = domParser.parseFromString(xmlString, 'text/xml');
    return xmlDoc;
}

/**
 * 
 */
function xPath(xmlDoc, path) {
    var retArray = [];

    var nodes = window.document.evaluate(path, xmlDoc, null, 0, null); 
    
    // -- traverse node -- //
    var rec = nodes.iterateNext();
    while (rec) {
        retArray.push(rec);
        rec = nodes.iterateNext();
    }

    return retArray;
}

/**
 * 
 */
function getXNodeVal(xmlElement, nodeName, includeChildren) {
    var gotten = xPath(xmlElement, 'descendant-or-self::' + nodeName + '/text()');
    var final = '';

    if (gotten.length > 0) {
        
        if (includeChildren === true) {
            for (var i = 0; i < gotten.length; i++) {
                final += gotten[i].nodeValue;
            }
        } 
        else {
            final = gotten[0].nodeValue;
        }
    }
    else {
        final = undefined;
    }
    return final;
}

// -- instance based --//
module.exports = HiveHelper;
