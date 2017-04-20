System.register([], function (_export, _context) {
    "use strict";

    var ResultValueConverter;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export('ResultValueConverter', ResultValueConverter = function () {
                function ResultValueConverter() {
                    _classCallCheck(this, ResultValueConverter);
                }

                ResultValueConverter.prototype.toView = function toView(value) {
                    if (!value) {
                        return 'not available';
                    }

                    if (value.status !== "FINISHED") {
                        return value.status;
                    }
                    return value.count < 0 ? '<=10' : value.count;
                };

                return ResultValueConverter;
            }());

            _export('ResultValueConverter', ResultValueConverter);
        }
    };
});
//# sourceMappingURL=result-value.converter.js.map
