System.register([], function (_export, _context) {
    "use strict";

    var ResultStyleValueConverter;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    function isUnresolved(value) {
        var finishedStatus = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 'FINISHED';

        return !value || value.status !== finishedStatus;
    }

    function getColorValue(value) {
        var errorStatus = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 'ERROR';
        var errorColor = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : '#FF0000';
        var altColor = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : '#00FF00';

        return !value || value.status === errorStatus ? errorColor : altColor;
    }
    return {
        setters: [],
        execute: function () {
            _export('ResultStyleValueConverter', ResultStyleValueConverter = function () {
                function ResultStyleValueConverter() {
                    _classCallCheck(this, ResultStyleValueConverter);
                }

                ResultStyleValueConverter.prototype.toView = function toView(value) {
                    var result = isUnresolved(value) ? 'color:' + getColorValue(value) : '';
                    return result;
                };

                return ResultStyleValueConverter;
            }());

            _export('ResultStyleValueConverter', ResultStyleValueConverter);
        }
    };
});
//# sourceMappingURL=result-style.converter.js.map
