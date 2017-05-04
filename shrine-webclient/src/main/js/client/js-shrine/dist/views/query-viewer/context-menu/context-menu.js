System.register(['aurelia-framework', 'common/i2b2.service.js'], function (_export, _context) {
    "use strict";

    var inject, bindable, I2B2Service, _dec, _class, _desc, _value, _class2, _descriptor, ContextMenu;

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
            inject = _aureliaFramework.inject;
            bindable = _aureliaFramework.bindable;
        }, function (_commonI2b2ServiceJs) {
            I2B2Service = _commonI2b2ServiceJs.I2B2Service;
        }],
        execute: function () {
            _export('ContextMenu', ContextMenu = (_dec = inject(I2B2Service), _dec(_class = (_class2 = function ContextMenu(i2b2Svc) {
                var _this = this;

                _classCallCheck(this, ContextMenu);

                _initDefineProp(this, 'context', _descriptor, this);

                this.loadQuery = function (id) {
                    i2b2Svc.loadQuery(id);
                    _this.context.class = 'hide';
                };
            }, (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'context', [bindable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('ContextMenu', ContextMenu);
        }
    };
});
//# sourceMappingURL=context-menu.js.map
