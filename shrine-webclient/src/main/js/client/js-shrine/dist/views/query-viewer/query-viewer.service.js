System.register(['aurelia-framework', 'aurelia-fetch-client', 'fetch'], function (_export, _context) {
    "use strict";

    var inject, HttpClient, _createClass, _dec, _class, maxNodesPerScreen, QueryViewerService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_aureliaFetchClient) {
            HttpClient = _aureliaFetchClient.HttpClient;
        }, function (_fetch) {}],
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

            maxNodesPerScreen = 10;

            _export('QueryViewerService', QueryViewerService = (_dec = inject(HttpClient, 'shrine'), _dec(_class = function () {
                function QueryViewerService(http, shrine) {
                    var _this = this;

                    _classCallCheck(this, QueryViewerService);

                    if (http !== undefined) {
                        http.configure(function (config) {
                            config.useStandardConfiguration().withBaseUrl(_this.url).withDefaults({
                                headers: {
                                    'Authorization': 'Basic ' + shrine.auth
                                }
                            });
                        });

                        this.http = http;
                    }
                }

                QueryViewerService.prototype.fetchPreviousQueries = function fetchPreviousQueries() {
                    return this.http.fetch('qep/queryResults').then(function (response) {
                        return response.json();
                    }).catch(function (error) {
                        return error;
                    });
                };

                QueryViewerService.prototype.getScreens = function getScreens(nodes, queries) {
                    var _this2 = this;

                    return new Promise(function (resolve, reject) {
                        var lastNodeIndex = nodes.sort().length;
                        var screens = [];
                        for (var i = 0; i < lastNodeIndex; i = i + maxNodesPerScreen) {
                            var numberOfNodesOnScreen = _this2.getNumberOfNodesOnScreen(nodes, i, maxNodesPerScreen);
                            var endIndex = numberOfNodesOnScreen - 1;
                            var screenId = _this2.getScreenId(nodes, i, endIndex);
                            var screenNodes = nodes.slice(i, numberOfNodesOnScreen);
                            var screenNodesToQueriesMap = _this2.mapQueriesToScreenNodes(screenNodes, queries, _this2.findQueriesForNode);
                            screens.push({
                                id: screenId,
                                nodes: screenNodes,
                                results: screenNodesToQueriesMap
                            });
                        }
                        resolve(screens);
                    });
                };

                QueryViewerService.prototype.mapQueriesToScreenNodes = function mapQueriesToScreenNodes(nodes, queries) {
                    var results = [];
                    queries.forEach(function (q, i) {
                        var result = {
                            name: q.query.queryName,
                            id: q.query.networkId,
                            nodeResults: []
                        };
                        nodes.forEach(function (n) {
                            result.nodeResults.push(q.adaptersToResults.find(function (a) {
                                return a.adapterNode === n;
                            }));
                        });
                        results.push(result);
                    });
                    return results;
                };

                QueryViewerService.prototype.getNumberOfNodesOnScreen = function getNumberOfNodesOnScreen(nodes, startIndex) {
                    var numNodes = startIndex + maxNodesPerScreen;
                    return numNodes < nodes.length ? numNodes : nodes.length;
                };

                QueryViewerService.prototype.getScreenId = function getScreenId(nodes, start, end) {
                    var startNode = nodes[start];
                    var endNode = nodes[end];
                    return String(startNode).substr(0, 1) + '-' + String(endNode).substr(0, 1);
                };

                _createClass(QueryViewerService, [{
                    key: 'url',
                    get: function get() {
                        var url = document.URL;
                        var service = ':6443/shrine-metadata/';
                        return url.substring(0, url.lastIndexOf(':')) + service;
                    }
                }]);

                return QueryViewerService;
            }()) || _class));

            _export('QueryViewerService', QueryViewerService);
        }
    };
});
//# sourceMappingURL=query-viewer.service.js.map
