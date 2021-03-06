'use strict';

System.register([], function (_export, _context) {
    "use strict";

    var CountValueConverter;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export('CountValueConverter', CountValueConverter = function () {
                function CountValueConverter() {
                    _classCallCheck(this, CountValueConverter);
                }

                CountValueConverter.prototype.toView = function toView(value) {
                    var PLUS_MINUS_CHAR = '\xB1';
                    return value < 0 ? '10 patients or fewer' : value + ' ' + PLUS_MINUS_CHAR + ' 10 patients';
                };

                return CountValueConverter;
            }());

            _export('CountValueConverter', CountValueConverter);
        }
    };
});
//# sourceMappingURL=count-value-converter.js.map
