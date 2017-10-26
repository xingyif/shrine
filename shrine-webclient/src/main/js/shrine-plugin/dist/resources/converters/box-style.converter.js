'use strict';

System.register([], function (_export, _context) {
    "use strict";

    var BoxStyleValueConverter;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export('BoxStyleValueConverter', BoxStyleValueConverter = function () {
                function BoxStyleValueConverter() {
                    _classCallCheck(this, BoxStyleValueConverter);
                }

                BoxStyleValueConverter.prototype.toView = function toView(value) {
                    return 'transform: translate(' + String(-100 * value) + '%);';
                };

                return BoxStyleValueConverter;
            }());

            _export('BoxStyleValueConverter', BoxStyleValueConverter);
        }
    };
});
//# sourceMappingURL=box-style.converter.js.map
