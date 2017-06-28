System.register(['aurelia-framework'], function (_export, _context) {
    "use strict";

    var bindable, inject, _createClass, _dec, _class, _desc, _value, _class2, _descriptor, Paginator;

    function _initDefineProp(target, property, descriptor, context) {
        if (!descriptor) return;
        Object.defineProperty(target, property, {
            enumerable: descriptor.enumerable,
            configurable: descriptor.configurable,
            writable: descriptor.writable,
            value: descriptor.initializer ? descriptor.initializer.call(context) : void 0
        });
    }

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    function _applyDecoratedDescriptor(target, property, decorators, descriptor, context) {
        var desc = {};
        Object['ke' + 'ys'](descriptor).forEach(function (key) {
            desc[key] = descriptor[key];
        });
        desc.enumerable = !!desc.enumerable;
        desc.configurable = !!desc.configurable;

        if ('value' in desc || desc.initializer) {
            desc.writable = true;
        }

        desc = decorators.slice().reverse().reduce(function (desc, decorator) {
            return decorator(target, property, desc) || desc;
        }, desc);

        if (context && desc.initializer !== void 0) {
            desc.value = desc.initializer ? desc.initializer.call(context) : void 0;
            desc.initializer = undefined;
        }

        if (desc.initializer === void 0) {
            Object['define' + 'Property'](target, property, desc);
            desc = null;
        }

        return desc;
    }

    function _initializerWarningHelper(descriptor, context) {
        throw new Error('Decorating class property failed. Please ensure that transform-class-properties is enabled.');
    }

    return {
        setters: [function (_aureliaFramework) {
            bindable = _aureliaFramework.bindable;
            inject = _aureliaFramework.inject;
        }],
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

            _export('Paginator', Paginator = (_dec = inject(Element), _dec(_class = (_class2 = function () {
                function Paginator(element) {
                    var _this = this;

                    _classCallCheck(this, Paginator);

                    _initDefineProp(this, 'pages', _descriptor, this);

                    Paginator.prototype.init = function () {
                        _this.index = 0;
                        _this.element = element;
                    };
                    this.init();
                }

                _createClass(Paginator, [{
                    key: 'pageIndex',
                    set: function set(i) {
                        var max = this.pages.length - 1;
                        this.index = i < 0 ? 0 : i > max ? max : i;
                        this.element.dispatchEvent(new CustomEvent('paginator-click', {
                            detail: { index: this.index },
                            bubbles: true,
                            cancelable: true
                        }));
                    }
                }]);

                return Paginator;
            }(), (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'pages', [bindable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('Paginator', Paginator);
        }
    };
});
//# sourceMappingURL=paginator.js.map
