"use strict";

System.register(["tape"], function (_export, _context) {
    "use strict";

    var spec;
    return {
        setters: [function (_tape) {
            spec = _tape;
        }],
        execute: function () {

            spec.test("App should exist: ", function (t) {
                t.true(true);
                t.end();
            });
        }
    };
});
//# sourceMappingURL=shell.spec.js.map
