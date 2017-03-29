
var wgxpath = require('wgxpath');
var Promise = require('bluebird');
var jsdom = require('node-jsdom');
var DOMParser = require('xmldom').DOMParser;
var fs = Promise.promisifyAll(require('fs'));
var path = require('path');
var HiveHelper = require('./hive-helper');

var clientLoadCallback;
var hiveHelper;

function MockDOM() {
    //expose to client.
    dom = this;
}

MockDOM.prototype.load = load;
MockDOM.prototype.loadData = loadData;


/**
 * load files.
 */
function load(url, jsIncludes, onLoad) {
    clientLoadCallback = onLoad;
    jsdom.env(url, jsIncludes, main);
}

/**
 * called when files are loaded.
 */
function main(errors, window) {
    
    // -- setup other items such as window. -- //
    wgxpath.install(window, true);
    window.DOMParser = DOMParser;
    hiveHelper = new HiveHelper(window);
    
    dom.hiveHelper = hiveHelper;
    dom.window = window;

    // -- core dom ready -- //
    clientLoadCallback(window);
}

/**
 * utility for loading absolute path.
 */
function getAbsolutePath(relativePath) {
    return path.join(__dirname, relativePath);
}


function loadData(filename) {
    var absPath = getAbsolutePath(filename);

    // -- create a promise after loading and parsing the file. --//
    var promise = new Promise(function (resolve, reject) {

        fs.readFileAsync(absPath)
            .then(function (data) {
                var str = String(data);

                var results = {
                    error: false,
                    data: str
                };

                resolve(results);
            });
    });

    return promise;
}


// -- singleton -- //
module.exports = new MockDOM();

