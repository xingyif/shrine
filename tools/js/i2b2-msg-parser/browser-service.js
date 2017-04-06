//https://www.npmjs.com/package/mock-browser

//https://developer.mozilla.org/en-US/docs/Web/API/DOMImplementation/createDocument
//https://www.npmjs.com/package/xmldom


var MockBrowser = require('mock-browser').mocks.MockBrowser;
var DOMParser = require('xmldom').DOMParser;

function BrowserService() {
    this.DOMParser = DOMParser;
    this.browser = new MockBrowser();
}

module.exports = BrowserService;

