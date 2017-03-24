System.register(['aurelia-framework', 'views/query-viewer/query-viewer.service'], function (_export, _context) {
    "use strict";

    var inject, QueryViewerService, _createClass, _dec, _class, nodesPerScreen, QueryViewer;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    function getNodes(queries) {
        return queries.length > 0 ? queries[0].results.map(function (result) {
            return result.node;
        }) : [];
    }

    function sliceResultsForScreen(queries, start, end) {
        return queries.map(function (query) {
            var q = Object.assign({}, query);
            q.results = query.results.slice(start, end);
            return q;
        });
    }
    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_viewsQueryViewerQueryViewerService) {
            QueryViewerService = _viewsQueryViewerQueryViewerService.QueryViewerService;
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

            nodesPerScreen = 5;

            _export('QueryViewer', QueryViewer = (_dec = inject(QueryViewerService), _dec(_class = function () {
                function QueryViewer(service) {
                    var _this = this;

                    _classCallCheck(this, QueryViewer);

                    this.screenIndex = 0;
                    this.service = service;
                    this.nodes = [];
                    this.service.fetchPreviousQueries().then(function (result) {
                        _this.queries = result.queries;
                        if (_this.nodes.length === 0) {
                            _this.nodes = getNodes(_this.queries);
                        }

                        _this.screenIndex = 1;
                        _this.screenNodes = _this.nodes.slice(_this.sliceStart, _this.sliceEnd);
                        _this.testQueries = sliceResultsForScreen(_this.queries, _this.sliceStart, _this.sliceEnd);
                    }).catch(function (error) {
                        return console.log(error);
                    });
                }

                _createClass(QueryViewer, [{
                    key: 'screens',
                    get: function get() {
                        var lastNodeIndex = this.nodes.length;
                        var testResult = [];

                        for (var i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
                            var start = this.nodes[i];
                            var endIndex = i + nodesPerScreen < lastNodeIndex ? i + nodesPerScreen : lastNodeIndex - 1;
                            var end = this.nodes[endIndex];
                            testResult.push(String(start).substr(0, 1) + '-' + String(end).substr(0, 1));
                        }

                        return testResult;
                    }
                }, {
                    key: 'sliceStart',
                    get: function get() {
                        return this.screenIndex * nodesPerScreen;
                    }
                }, {
                    key: 'sliceEnd',
                    get: function get() {
                        return this.sliceStart + nodesPerScreen;
                    }
                }]);

                return QueryViewer;
            }()) || _class));

            _export('QueryViewer', QueryViewer);
        }
    };
});
//# sourceMappingURL=query-viewer.js.map
