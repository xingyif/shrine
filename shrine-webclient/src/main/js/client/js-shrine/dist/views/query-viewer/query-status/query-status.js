System.register(['aurelia-framework', 'ramda'], function (_export, _context) {
    "use strict";

    var bindable, inject, _, _dec, _class, _desc, _value, _class2, _descriptor, QueryStatus;

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
        }, function (_ramda) {
            _ = _ramda;
        }],
        execute: function () {
            _export('QueryStatus', QueryStatus = (_dec = inject(Math), _dec(_class = (_class2 = function () {
                function QueryStatus(Math) {
                    _classCallCheck(this, QueryStatus);

                    _initDefineProp(this, 'status', _descriptor, this);

                    this.floor = Math.floor;
                }

                QueryStatus.prototype.attached = function attached() {
                    var svgScaleMultiplier = 75;
                    var scaleToSVG = _.curry(function (f, m, t, n) {
                        return f(n / t * m);
                    })(this.floor, svgScaleMultiplier, this.status.total);

                    var status = this.status;
                    var finishedPct = scaleToSVG(status.finished);
                    var errorPct = scaleToSVG(status.error);
                    this.readyOffset = 100 - finishedPct;
                    this.errorOffset = this.readyOffset - errorPct;
                    this.finished = status.finished;
                    this.error = status.error;
                    this.pending = status.total - (status.finished + status.error);
                    this.total = status.total;
                };

                return QueryStatus;
            }(), (_descriptor = _applyDecoratedDescriptor(_class2.prototype, 'status', [bindable], {
                enumerable: true,
                initializer: null
            })), _class2)) || _class));

            _export('QueryStatus', QueryStatus);
        }
    };
});
//# sourceMappingURL=query-status.js.map
