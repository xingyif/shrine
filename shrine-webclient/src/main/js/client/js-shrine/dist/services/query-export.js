System.register(['./pub-sub'], function (_export, _context) {
    "use strict";

    var PubSub, QueryExport, convertObjectToCSV, exportIEWebkitGecko;

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
            _export('QueryExport', QueryExport = function (_PubSub) {
                _inherits(QueryExport, _PubSub);

                function QueryExport() {
                    _classCallCheck(this, QueryExport);

                    for (var _len = arguments.length, rest = Array(_len), _key = 0; _key < _len; _key++) {
                        rest[_key] = arguments[_key];
                    }

                    return _possibleConstructorReturn(this, _PubSub.call.apply(_PubSub, [this].concat(rest)));
                }

                QueryExport.prototype.listen = function listen() {
                    this.subscribe(this.commands.shrine.exportResult, convertObjectToCSV);
                };

                return QueryExport;
            }(PubSub));

            _export('QueryExport', QueryExport);

            convertObjectToCSV = function convertObjectToCSV(d) {
                var nodes = d.nodes.sort();
                var m = new Map();
                var desc = function desc(_ref) {
                    var description = _ref.resultType.i2b2Options.description;
                    return description;
                };
                var brdSort = function brdSort(a, b) {
                    return desc(a) <= desc(b) ? -1 : 1;
                };
                nodes.forEach(function (_ref2) {
                    var _ref2$breakdowns = _ref2.breakdowns,
                        breakdowns = _ref2$breakdowns === undefined ? [] : _ref2$breakdowns;

                    breakdowns.sort(brdSort).forEach(function (_ref3) {
                        var _m$get;

                        var description = _ref3.resultType.i2b2Options.description,
                            results = _ref3.results;
                        return m.has(description) ? (_m$get = m.get(description)).add.apply(_m$get, results.map(function (r) {
                            return r.dataKey;
                        }).sort()) : m.set(description, new Set(results.map(function (r) {
                            return r.dataKey;
                        }).sort()));
                    });
                });

                var line1 = 'SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),' + [''].concat(nodes.map(function (n) {
                    return n.adapterNode;
                }).join(','));
                var line2 = '\nAll Patients,' + [''].concat(nodes.map(function (n) {
                    return n.count ? n.count > 0 ? n.count : 0 : 'unavailable';
                }).join(','));
                var result = [];
                m.forEach(function (v, k) {
                    result.push.apply(result, [''].concat(Array.from(v).map(function (s) {
                        var title = k.split(' ').shift() + ',' + s;
                        var values = nodes.map(function (_ref4) {
                            var _ref4$breakdowns = _ref4.breakdowns,
                                breakdowns = _ref4$breakdowns === undefined ? [] : _ref4$breakdowns;

                            var b = breakdowns.find(function (_ref5) {
                                var description = _ref5.resultType.i2b2Options.description,
                                    results = _ref5.results;
                                return description === k;
                            });
                            var r = b ? b.results.find(function (r) {
                                return r.dataKey === s;
                            }) : undefined;
                            return !r ? 'unavailable' : r.value > 0 ? r.value : 0;
                        });
                        return title + ',' + values.join(",");
                    })));
                });
                var csv = '' + line1 + line2 + result.join('\n');
                exportIEWebkitGecko(csv);
            };

            exportIEWebkitGecko = function exportIEWebkitGecko(csv) {
                var filename = "export.csv";
                var blob = new Blob([csv]);
                var isIE = window.navigator !== undefined && window.navigator.msSaveOrOpenBlob !== undefined;
                if (isIE) window.navigator.msSaveBlob(blob, filename);else {
                    var a = window.document.createElement("a");
                    a.href = window.URL.createObjectURL(blob, { type: "text/plain" });
                    a.download = filename;
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                }
            };
        }
    };
});
//# sourceMappingURL=query-export.js.map
