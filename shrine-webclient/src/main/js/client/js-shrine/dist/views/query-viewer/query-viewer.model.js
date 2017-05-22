System.register([], function (_export, _context) {
    "use strict";

    var _createClass, QueryViewerModel;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _createClass = function () {
                function defineProperties(target, props) {
                    for (var i = 0; i < props.length; i++) {
                        var descriptor = props[i];
                        descriptor.enumerable = descriptor.enumerable || false;
                        descriptor.configurable = true;
                        if ("value" in descriptor) descriptor.writable = true;
                        Object.defineProperty(target, descriptor.key, descriptor);
                    }
                }

                return function (Constructor, protoProps, staticProps) {
                    if (protoProps) defineProperties(Constructor.prototype, protoProps);
                    if (staticProps) defineProperties(Constructor, staticProps);
                    return Constructor;
                };
            }();

            _export("QueryViewerModel", QueryViewerModel = function () {
                function QueryViewerModel() {
                    _classCallCheck(this, QueryViewerModel);

                    this.isLoaded = false;
                    this.loadedCount = 0;
                    this.totalQueries = 0;
                    this.screens = [];
                }

                _createClass(QueryViewerModel, [{
                    key: "moreToLoad",
                    get: function get() {
                        console.log("loaded count " + this.loadedCount + " total queris: " + this.totalQueries);
                        return this.loadedCount < this.totalQueries;
                    }
                }]);

                return QueryViewerModel;
            }());

            _export("QueryViewerModel", QueryViewerModel);
        }
    };
});
//# sourceMappingURL=query-viewer.model.js.map
