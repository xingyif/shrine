'use strict';

System.register(['moment'], function (_export, _context) {
  "use strict";

  var moment, DateTimeValueConverter;

  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  return {
    setters: [function (_moment) {
      moment = _moment.default;
    }],
    execute: function () {
      _export('DateTimeValueConverter', DateTimeValueConverter = function () {
        function DateTimeValueConverter() {
          _classCallCheck(this, DateTimeValueConverter);
        }

        DateTimeValueConverter.prototype.toView = function toView(value) {
          return moment(value).format('MM/DD/YYYY h:mm:ss a');
        };

        return DateTimeValueConverter;
      }());

      _export('DateTimeValueConverter', DateTimeValueConverter);
    }
  };
});
//# sourceMappingURL=datetime.value.converter.js.map
