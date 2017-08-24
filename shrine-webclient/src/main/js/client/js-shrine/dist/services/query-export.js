System.register(['./pub-sub'], function (_export, _context) {
    "use strict";

    var PubSub, QueryExport, convertObjectToCSV;

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
                var nodeNames = d.nodes.map(function (n) {
                    return n.adapterNode;
                });
                var nodes = d.nodes;
                var m = new Map();
                nodes.forEach(function (_ref) {
                    var breakdowns = _ref.breakdowns;
                    return breakdowns.forEach(function (_ref2) {
                        var _m$get;

                        var description = _ref2.resultType.i2b2Options.description,
                            results = _ref2.results;
                        return m.has(description) ? (_m$get = m.get(description)).add.apply(_m$get, results.map(function (r) {
                            return r.dataKey;
                        })) : m.set(description, new Set(results.map(function (r) {
                            return r.dataKey;
                        })));
                    });
                });

                var line1 = 'data:text/csv;charset=utf-8,SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS),' + nodes.map(function (n) {
                    return n.adapterNode;
                }).join(',');
                var line2 = '\nAll Patients,' + nodes.map(function (n) {
                    return n.count;
                }).join(',');
                var result = [];
                m.forEach(function (v, k) {
                    result.push.apply(result, [''].concat(Array.from(v).map(function (s) {
                        var title = k.split(' ').shift() + '|' + s;
                        var values = nodes.map(function (_ref3) {
                            var breakdowns = _ref3.breakdowns;

                            var b = breakdowns.find(function (_ref4) {
                                var description = _ref4.resultType.i2b2Options.description,
                                    results = _ref4.results;
                                return description === k;
                            });
                            var r = b ? b.results.find(function (r) {
                                return r.dataKey === s;
                            }) : undefined;
                            return r ? r.value : 'unavailable';
                        });
                        return title + ',' + values.join(",");
                    })));
                });
                var csv = encodeURI('' + line1 + line2 + result.join('\n'));
                var link = document.createElement('a');
                link.setAttribute('href', csv);
                link.setAttribute('download', 'export.csv');
                link.click();
            };
        }
    };
});
//# sourceMappingURL=query-export.js.map
