'use strict';

System.register(['aurelia-framework'], function (_export, _context) {
    "use strict";

    var bindable, customElement, _dec, _class, _desc, _value, _class2, _descriptor, Breakdown;

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
            customElement = _aureliaFramework.customElement;
        }],
        execute: function () {
            _export('Breakdown', Breakdown = (_dec = customElement('breakdown'), _dec(_class = (_class2 = function Breakdown() {
                _classCallCheck(this, Breakdown);

                _initDefineProp(this, 'data', _descriptor, this);
            }, (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'data', [bindable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('Breakdown', Breakdown);
        }
    };
});
//# sourceMappingURL=breakdown.js.map
