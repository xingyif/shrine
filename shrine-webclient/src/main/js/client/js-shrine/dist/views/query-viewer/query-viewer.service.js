System.register(['aurelia-framework', 'repository/qep.repository', './query-viewer.config'], function (_export, _context) {
    "use strict";

    var inject, QEPRepository, QueryViewerConfig, _dec, _class, QueryViewerService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_repositoryQepRepository) {
            QEPRepository = _repositoryQepRepository.QEPRepository;
        }, function (_queryViewerConfig) {
            QueryViewerConfig = _queryViewerConfig.QueryViewerConfig;
        }],
        execute: function () {
            _export('QueryViewerService', QueryViewerService = (_dec = inject(QEPRepository, QueryViewerConfig), _dec(_class = function () {
                function QueryViewerService(repository, config, i2b2Svc) {
                    _classCallCheck(this, QueryViewerService);

                    this.repository = repository;
                    this.config = config;
                }

                QueryViewerService.prototype.fetchPreviousQueries = function fetchPreviousQueries() {
                    var limit = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : this.config.maxQueriesPerScroll;

                    return this.repository.fetchPreviousQueries(limit);
                };

                QueryViewerService.prototype.getScreens = function getScreens(nodes, queries) {
                    var _this = this;

                    return new Promise(function (resolve, reject) {
                        var lastNodeIndex = nodes.sort().length;
                        var screens = [];
                        for (var i = 0; i < lastNodeIndex; i = i + _this.config.maxNodesPerScreen) {
                            var numberOfNodesOnScreen = _this.getNumberOfNodesOnScreen(nodes, i, _this.config.maxNodesPerScreen);
                            var endIndex = numberOfNodesOnScreen - 1;
                            var screenId = _this.getScreenId(nodes, i, endIndex);
                            var screenNodes = nodes.slice(i, numberOfNodesOnScreen);
                            var screenNodesToQueriesMap = _this.mapQueriesToScreenNodes(screenNodes, queries, _this.findQueriesForNode);
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
                    var numNodes = startIndex + this.config.maxNodesPerScreen;
                    return numNodes < nodes.length ? numNodes : nodes.length;
                };

                QueryViewerService.prototype.getScreenId = function getScreenId(nodes, start, end) {
                    var startNode = nodes[start];
                    var endNode = nodes[end];
                    return String(startNode).substr(0, 1) + '-' + String(endNode).substr(0, 1);
                };

                return QueryViewerService;
            }()) || _class));

            _export('QueryViewerService', QueryViewerService);
        }
    };
});
//# sourceMappingURL=query-viewer.service.js.map
