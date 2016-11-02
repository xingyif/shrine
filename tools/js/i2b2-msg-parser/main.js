var dom = require('./i2b2-dom');
var HiveHelper = require('./hive-helper');
var url = './html/index.html';
var QryStatusCtrlr = require('./crc-ctrlr-qry-status');

var i2b2Ext = [
    'js-ext/lodash/lodash.min.js',
    'js-ext/yui/build/yahoo/yahoo.js',
    'js-ext/yui/build/event/event.js',
    'js-ext/yui/build/dom/dom.js',
    'js-ext/yui/build/yuiloader/yuiloader.js',
    'js-ext/yui/build/dragdrop/dragdrop.js',
    'js-ext/yui/build/element/element.js',
    'js-ext/yui/build/container/container_core.js',
    'js-ext/yui/build/container/container.js',
    'js-ext/yui/build/resize/resize.js',
    'js-ext/yui/build/utilities/utilities.js',
    'js-ext/yui/build/menu/menu.js',
    'js-ext/yui/build/calendar/calendar.js',
    'js-ext/yui/build/treeview/treeview.js',
    'js-ext/yui/build/tabview/tabview.js',
    'js-ext/yui/build/animation/animation.js',
    'js-ext/yui/build/datasource/datasource.js',
    'js-ext/yui/build/yahoo-dom-event/yahoo-dom-event.js',
    'js-ext/yui/build/json/json-min.js',
    'js-ext/yui/build/datatable/datatable.js',
    'js-ext/yui/build/button/button.js',
    'js-ext/yui/build/paginator/paginator-min.js',
    'js-ext/yui/build/slider/slider-min.js',
    'js-ext/jquerycode/jquery-1.11.1.js',
    'js-ext/idle-timer.js',
    'js-ext/YUI_DataTable_PasswordCellEditor.js',
    'js-ext/YUI_DataTable_MD5CellEditor.js',
    'js-ext/prototype.js',
    'js-ext/firebug/firebugx.js',
    'js-ext/excanvas.js',
    'js-ext/bubbling-min.js',
    'js-ext/accordion-min.js'
];

var i2b2Js = [
    'js-i2b2/i2b2_loader.js',
    'js-i2b2/hive/hive.ui.js',
    'js-i2b2/cells/SHRINE/EnhancedError.js'
];

dom.load(url, i2b2Ext.concat(i2b2Js), domLoaded);

function domLoaded(window) {

    var $ = window.jQuery;
    QryStatusCtrlr.seedDOM(window);

    dom.mockMessageResult('query-contents.xml')
        .then(function (results) {
            
           //QryStatusCtrlr.refreshStatusCallback(results);
           var qi_list = results.refXML.getElementsByTagName('queryDefinition');  //Original code commented by BG

        });
}
