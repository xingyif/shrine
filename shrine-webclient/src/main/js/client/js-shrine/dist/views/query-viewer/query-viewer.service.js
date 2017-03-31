System.register(['aurelia-framework', 'aurelia-fetch-client', 'fetch'], function (_export, _context) {
    "use strict";

    var inject, HttpClient, _createClass, _dec, _class, nodesPerScreen, QueryViewerService;

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

            nodesPerScreen = 10;

            _export('QueryViewerService', QueryViewerService = (_dec = inject(HttpClient), _dec(_class = function () {
                function QueryViewerService(http) {
                    var _this = this;

                    _classCallCheck(this, QueryViewerService);

                    http.configure(function (config) {
                        config.useStandardConfiguration().withBaseUrl(_this.url);
                    });

                    this.http = http;
                }

                QueryViewerService.prototype.fetchPreviousQueries = function fetchPreviousQueries() {
                    return this.http.fetch('previous-queries').then(function (response) {
                        return response.json();
                    }).catch(function (error) {
                        return error;
                    });
                };

                QueryViewerService.prototype.getNodes = function getNodes(queries) {
                    return queries.length > 0 ? queries[0].results.map(function (result) {
                        return result.node;
                    }) : [];
                };

                QueryViewerService.prototype.getScreens = function getScreens(nodes, queries) {
                    return new Promise(function (resolve, reject) {
                        var lastNodeIndex = nodes.length;
                        var screens = [];

                        var _loop = function _loop(i) {
                            var endIndex = i + nodesPerScreen < lastNodeIndex ? i + nodesPerScreen : lastNodeIndex - 1;
                            var screenId = String(nodes[i]).substr(0, 1) + '-' + String(nodes[endIndex]).substr(0, 1);
                            var screenNodes = nodes.slice(i, endIndex);
                            var screenQueries = queries.map(function (query) {
                                return {
                                    id: query.id,
                                    name: query.name,
                                    results: query.results.slice(i, endIndex)
                                };
                            });

                            screens.push({
                                name: screenId,
                                nodes: screenNodes,
                                queries: screenQueries
                            });
                        };

                        for (var i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
                            _loop(i);
                        }

                        resolve(screens);
                    });
                };

                _createClass(QueryViewerService, [{
                    key: 'url',
                    get: function get() {
                        var port = '8000';
                        var url = document.URL;
                        var service = '6443/shrine-proxy/request/shrine/api/';
                        return url.substring(0, url.indexOf(port)) + service;
                    }
                }]);

                return QueryViewerService;
            }()) || _class));

            _export('QueryViewerService', QueryViewerService);
        }
    };
});
//# sourceMappingURL=query-viewer.service.js.map
