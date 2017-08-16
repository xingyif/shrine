System.register(['./pub-sub'], function (_export, _context) {
    "use strict";

    var PubSub, privateProps, Export, convertObjectToCSV;

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

    return {
        setters: [function (_pubSub) {
            PubSub = _pubSub.PubSub;
        }],
        execute: function () {
            privateProps = new WeakMap();

            _export('Export', Export = function (_PubSub) {
                _inherits(Export, _PubSub);

                function Export() {
                    _classCallCheck(this, Export);

                    for (var _len = arguments.length, rest = Array(_len), _key = 0; _key < _len; _key++) {
                        rest[_key] = arguments[_key];
                    }

                    var _this = _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));

                    privateProps.set(_this, {});
                    return _this;
                }

                Export.prototype.listen = function listen() {
                    this.subscribe(this.commands.shrine.exportResult, convertObjectToCSV);
                };

                return Export;
            }(PubSub));

            _export('Export', Export);

            convertObjectToCSV = function convertObjectToCSV(d) {

                var nodeNames = d.nodes.map(function (n) {
                    return n.adapterNode;
                });
                var nodes = d.nodes;

                var breakdownMap = new Map();
                nodes.map(function (_ref) {
                    var breakdowns = _ref.breakdowns,
                        adapterNode = _ref.adapterNode;
                    return breakdowns.map(function (_ref2) {
                        var description = _ref2.resultType.i2b2Options.description;
                        return breakdownMap.set(description, {});
                    });
                });

                for (var _iterator = breakdownMap, _isArray = Array.isArray(_iterator), _i = 0, _iterator = _isArray ? _iterator : _iterator[Symbol.iterator]();;) {
                    var _ref3;

                    if (_isArray) {
                        if (_i >= _iterator.length) break;
                        _ref3 = _iterator[_i++];
                    } else {
                        _i = _iterator.next();
                        if (_i.done) break;
                        _ref3 = _i.value;
                    }

                    var m = _ref3;

                    console.log(m);
                }

                var line1 = 'data:text/csv;charset=utf-8,SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),' + d.nodes.map(function (n) {
                    return n.adapterNode;
                }).join(',');
                var line2 = '\nAll Patients,' + d.nodes.map(function (n) {
                    return n.count;
                }).join(',');
                var csv = encodeURI('' + line1 + line2);
                var link = document.createElement('a');
                var breakdowns = d.nodes.map(function (n) {
                    return n.breakdowns;
                });

                link.setAttribute('href', csv);
                link.setAttribute('download', 'test.csv');
            };
        }
    };
});
//# sourceMappingURL=export.js.map
