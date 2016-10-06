

/**
 * TODO:  make this and extension of mock-dom
 */

var wgxpath = require('wgxpath');
var Promise = require('bluebird');
var jsdom = require('node-jsdom');
var DOMParser = require('xmldom').DOMParser;
var fs = Promise.promisifyAll(require('fs'));
var path = require('path');
var HiveHelper = require('./hive-helper');

var clientLoadCallback;
var hiveHelper;

function I2B2DOM() {
    //expose to client.
    dom = this;
}

I2B2DOM.prototype.load = load;
I2B2DOM.prototype.getAbsolutePath = getAbsolutePath;
I2B2DOM.prototype.loadData = loadData;
I2B2DOM.prototype.mockMessageResult = mockMessageResult;


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

    // -- replace i2b2.h with ours -- //
    if(window.i2b2) {
        window.i2b2.h = hiveHelper;
    }
    
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

/**
 * Return a promise that can be utilized after file is loaded and parsed.
 */
function mockMessageResult(filename) {

    var absPath = getAbsolutePath(filename);

    // -- create a promise after loading and parsing the file. --//
    var promise = new Promise(function (resolve, reject) {

        fs.readFileAsync(absPath)
            .then(function (data) {
                var str = String(data);

                var xmlDoc = hiveHelper.parseXml(str);

                var results = {
                    error: false,
                    refXML: xmlDoc
                };

                resolve(results);
            });
    });

    return promise;
}

// -- singleton -- //
module.exports = new I2B2DOM();

