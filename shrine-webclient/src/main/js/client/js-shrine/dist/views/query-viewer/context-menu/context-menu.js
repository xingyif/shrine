System.register(['aurelia-framework', 'aurelia-event-aggregator', 'common/shrine.messages'], function (_export, _context) {
    "use strict";

    var bindable, EventAggregator, commands, _desc, _value, _class, _descriptor, _class2, _temp, _initialiseProps, ContextMenu;

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
        }, function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_commonShrineMessages) {
            commands = _commonShrineMessages.commands;
        }],
        execute: function () {
            _export('ContextMenu', ContextMenu = (_class = (_temp = _class2 = function ContextMenu(evtAgg, commands) {
                var _this = this;

                _classCallCheck(this, ContextMenu);

                _initialiseProps.call(this);

                ContextMenu.prototype.cloneQuery = function (id) {
                    evtAgg.publish(commands.i2b2.cloneQuery, id);
                    _this.context.class = 'hide';
                };
                ContextMenu.prototype.renameQuery = function (id) {
                    evtAgg.publish(commands.i2b2.renameQuery, id);
                    _this.context.class = 'hide';
                };
                ContextMenu.prototype.flagQuery = function (id) {
                    evtAgg.publish(commands.i2b2.flagQuery, id);
                    _this.context.class = 'hide';
                };

                ContextMenu.prototype.unflagQuery = function (id) {
                    evtAgg.publish(commands.i2b2.unflagQuery, id);
                    _this.context.class = 'hide';
                };
            }, _class2.inject = [EventAggregator, commands], _initialiseProps = function _initialiseProps() {
                _initDefineProp(this, 'context', _descriptor, this);
            }, _temp), (_descriptor = _applyDecoratedDescriptor(_class.prototype, 'context', [bindable], {
                enumerable: true,
                initializer: null
            })), _class));

            _export('ContextMenu', ContextMenu);
        }
    };
});
//# sourceMappingURL=context-menu.js.map
