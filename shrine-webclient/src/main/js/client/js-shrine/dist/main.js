System.register([], function (_export, _context) {
    "use strict";

    function configure(aurelia) {
        aurelia.use.standardConfiguration().developmentLogging();

        aurelia.start().then(function () {
            return aurelia.setRoot('shell');
        });

        var shrine = {
            auth: sessionStorage.getItem('shrine.auth')
        };
        sessionStorage.removeItem('shrine.auth');
        aurelia.use.instance('shrine', shrine);
    }

    _export('configure', configure);

    return {
        setters: [],
        execute: function () {}
    };
});
//# sourceMappingURL=main.js.map
