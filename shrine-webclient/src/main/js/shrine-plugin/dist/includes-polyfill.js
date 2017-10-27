'use strict';

System.register([], function (_export, _context) {
  "use strict";

  var stringIncludesPolyfill;
  return {
    setters: [],
    execute: function () {
      _export('stringIncludesPolyfill', stringIncludesPolyfill = function stringIncludesPolyfill() {
        if (!String.prototype.includes) {
          String.prototype.includes = function (search, start) {
            'use strict';

            if (typeof start !== 'number') {
              start = 0;
            }

            if (start + search.length > this.length) {
              return false;
            } else {
              return this.indexOf(search, start) !== -1;
            }
          };
        }
      });

      _export('stringIncludesPolyfill', stringIncludesPolyfill);
    }
  };
});
//# sourceMappingURL=includes-polyfill.js.map
