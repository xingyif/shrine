"use strict";

System.register([], function (_export, _context) {
    "use strict";

    var TruncateValueConverter;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export("TruncateValueConverter", TruncateValueConverter = function () {
                function TruncateValueConverter() {
                    _classCallCheck(this, TruncateValueConverter);
                }

                TruncateValueConverter.prototype.toView = function toView(value) {
                    var max = 20;
                    return value.length > max ? value.substring(0, max) + "..." : value;
                };

                return TruncateValueConverter;
            }());

            _export("TruncateValueConverter", TruncateValueConverter);
        }
    };
});
//# sourceMappingURL=truncate.converter.js.map
