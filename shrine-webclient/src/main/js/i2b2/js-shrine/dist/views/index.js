'use strict';

System.register([], function (_export, _context) {
    "use strict";

    function configure(aurelia) {

        var views = ['views/query-status/query-status'];
        aurelia.globalResources.apply(aurelia, views);
    }

    _export('configure', configure);

    return {
        setters: [],
        execute: function () {}
    };
});
//# sourceMappingURL=index.js.map
