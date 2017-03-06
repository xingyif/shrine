System.register(['src/app.js'], function (_export, _context) {
    "use strict";

    var App, App2;
    return {
        setters: [function (_srcAppJs) {
            App = _srcAppJs.App;
            App2 = _srcAppJs.App2;
        }],
        execute: function () {

            describe('A test of app.js ', function () {
                it('should not work', function () {
                    var app = new App();
                    expect(app.message).toBe('Hello Aurelia');
                });
            });
        }
    };
});
//# sourceMappingURL=app.spec.js.map
