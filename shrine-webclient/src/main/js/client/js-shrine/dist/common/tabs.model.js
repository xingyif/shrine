System.register([], function (_export, _context) {
    "use strict";

    var _createClass, TabsModel;

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

            _export('TabsModel', TabsModel = function () {
                function TabsModel() {
                    _classCallCheck(this, TabsModel);

                    var mode = TabsModel.min;
                    TabsModel.prototype.setMax = function () {
                        return mode = TabsModel.full;
                    };
                    TabsModel.prototype.setMin = function () {
                        return mode = TabsModel.min;
                    };
                    TabsModel.prototype.mode = function () {
                        return mode;
                    };
                }

                _createClass(TabsModel, null, [{
                    key: 'full',
                    get: function get() {
                        return 'v-full';
                    }
                }, {
                    key: 'min',
                    get: function get() {
                        return 'v-min';
                    }
                }]);

                return TabsModel;
            }());

            _export('TabsModel', TabsModel);
        }
    };
});
//# sourceMappingURL=tabs.model.js.map
