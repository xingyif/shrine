System.register(['./includes-polyfill'], function (_export, _context) {
    "use strict";

    var stringIncludesPolyfill;
    function configure(aurelia) {
        aurelia.use.standardConfiguration().developmentLogging().feature('resources').feature('views');

        aurelia.start().then(stringIncludesPolyfill).then(function () {
            return aurelia.setRoot('shell');
        });
    }

    _export('configure', configure);

    return {
        setters: [function (_includesPolyfill) {
            stringIncludesPolyfill = _includesPolyfill.stringIncludesPolyfill;
        }],
        execute: function () {}
    };
});
//# sourceMappingURL=main.js.map
