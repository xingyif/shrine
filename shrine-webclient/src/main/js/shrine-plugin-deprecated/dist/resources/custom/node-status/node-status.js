'use strict';

System.register(['aurelia-framework', 'services/pub-sub'], function (_export, _context) {
    "use strict";

    var customElement, bindable, PubSub, _dec, _class, _desc, _value, _class2, _descriptor, NodeStatus;

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

    function _possibleConstructorReturn(self, call) {
        if (!self) {
            throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
        }

        return call && (typeof call === "object" || typeof call === "function") ? call : self;
    }

    function _inherits(subClass, superClass) {
        if (typeof superClass !== "function" && superClass !== null) {
            throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
        }

        subClass.prototype = Object.create(superClass && superClass.prototype, {
            constructor: {
                value: subClass,
                enumerable: false,
                writable: true,
                configurable: true
            }
        });
        if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
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
            customElement = _aureliaFramework.customElement;
            bindable = _aureliaFramework.bindable;
        }, function (_servicesPubSub) {
            PubSub = _servicesPubSub.PubSub;
        }],
        execute: function () {
            _export('NodeStatus', NodeStatus = (_dec = customElement('node-status'), _dec(_class = (_class2 = function (_PubSub) {
                _inherits(NodeStatus, _PubSub);

                function NodeStatus() {
                    _classCallCheck(this, NodeStatus);

                    for (var _len = arguments.length, rest = Array(_len), _key = 0; _key < _len; _key++) {
                        rest[_key] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    _initDefineProp(_this, 'result', _descriptor, _this);

                    return _this;
                }

                return NodeStatus;
            }(PubSub), (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'result', [bindable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('NodeStatus', NodeStatus);
        }
    };
});
//# sourceMappingURL=node-status.js.map
