System.register([], function (_export, _context) {
    "use strict";

    var _createClass, Container;

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

            _export("Container", Container = function () {
                function Container(v) {
                    _classCallCheck(this, Container);

                    this.__value = v;
                }

                Container.of = function of(value) {
                    return new Container(value);
                };

                Container.prototype.map = function map(f) {
                    return this.hasNothing() ? Container.of(null) : Container.of(f(this.value));
                };

                Container.prototype.hasNothing = function hasNothing() {
                    return this.value === null || this.value === undefined;
                };

                _createClass(Container, [{
                    key: "value",
                    get: function get() {
                        return this.__value;
                    }
                }]);

                return Container;
            }());

            _export("Container", Container);
        }
    };
});
//# sourceMappingURL=container.js.map
