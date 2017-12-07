'use strict';

System.register([], function (_export, _context) {
    "use strict";

    function configure(aurelia) {

        var converterPrefix = 'converters';
        var converters = ['box-style.converter', 'count-value-converter', 'datetime.value.converter', 'result-style.converter', 'result-value.converter', 'truncate.converter'];
        aurelia.globalResources.apply(aurelia, converters.map(function (c) {
            return './' + converterPrefix + '/' + c;
        }));

        var customPrefix = 'custom';
        var custom = ['breakdown/breakdown', 'node-result/node-result', 'node-status/node-status'];
        aurelia.globalResources.apply(aurelia, custom.map(function (c) {
            return './' + customPrefix + '/' + c;
        }));
    }

    _export('configure', configure);

    return {
        setters: [],
        execute: function () {}
    };
});
//# sourceMappingURL=index.js.map
