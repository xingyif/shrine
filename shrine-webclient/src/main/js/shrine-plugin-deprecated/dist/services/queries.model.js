'use strict';

System.register(['aurelia-event-aggregator', 'repository/qep.repository', './shrine.messages'], function (_export, _context) {
    "use strict";

    var EventAggregator, QEPRepository, notifications, _class, _temp, QueriesModel;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_repositoryQepRepository) {
            QEPRepository = _repositoryQepRepository.QEPRepository;
        }, function (_shrineMessages) {
            notifications = _shrineMessages.notifications;
        }],
        execute: function () {
            _export('QueriesModel', QueriesModel = (_temp = _class = function QueriesModel(evtAgg, QEPRepository, notifications) {
                var _this = this;

                _classCallCheck(this, QueriesModel);

                var qep = QEPRepository;
                var maxQueriesPerFetch = 40;
                var loadedCount = 0;
                var totalQueries = 0;
                var data = null;

                QueriesModel.prototype.load = function () {
                    return qep.fetchPreviousQueries(_this.maxQueriesPerFetch() + _this.loadedCount()).then(function (result) {
                        totalQueries = result.rowCount;
                        loadedCount = result.queryResults.length;
                        return result;
                    }).catch(function (error) {
                        return console.log(error);
                    }).then(toPages).then(function (pages) {
                        data = pages;
                        evtAgg.publish(notifications.shrine.queriesReceived, data);
                    });
                };

                QueriesModel.prototype.totalQueries = function () {
                    return totalQueries;
                };
                QueriesModel.prototype.loadedCount = function () {
                    return loadedCount;
                };
                QueriesModel.prototype.maxQueriesPerFetch = function () {
                    return maxQueriesPerFetch;
                };
                QueriesModel.prototype.moreToLoad = function () {
                    return loadedCount < totalQueries;
                };
                QueriesModel.prototype.hasData = function () {
                    return data !== null && data !== undefined;
                };

                var toPages = function toPages(data) {
                    var nodesPerPage = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 6;

                    return new Promise(function (resolve, reject) {
                        var pages = [];

                        var nodesPerPage = 6;
                        var nodes = data.adapters;
                        var lastNodeIndex = nodes.length;
                        var queries = data.queryResults;

                        for (var i = 0; i < lastNodeIndex; i = i + nodesPerPage) {
                            var numberOfNodes = geNumberOfNodes(nodes, i, nodesPerPage);
                            var pageNodes = nodes.slice(i, numberOfNodes);
                            var results = mapQueries(pageNodes, queries);
                            pages.push({
                                nodes: pageNodes,
                                results: results
                            });
                        }

                        resolve(pages);
                    });
                };

                var geNumberOfNodes = function geNumberOfNodes(nodes, startIndex, nodesPerPage) {
                    var numNodes = startIndex + nodesPerPage;
                    return numNodes < nodes.length ? numNodes : nodes.length;
                };

                var mapQueries = function mapQueries(nodes, queries) {
                    var results = [];
                    queries.forEach(function (q, i) {
                        var result = {
                            name: q.query.queryName,
                            id: q.query.networkId,
                            date: q.query.dateCreated,
                            flagged: q.query.flagged === true,
                            flagMessage: q.query.flagMessage || null,
                            nodeResults: [],
                            status: q.adaptersToResults.reduce(function (s, r) {
                                var finished = r.status === "FINISHED" ? s.finished + 1 : s.finished;
                                var error = r.status === "ERROR" ? s.error + 1 : s.error;
                                return { error: error, finished: finished, total: q.adaptersToResults.length };
                            }, { error: 0, finished: 0, total: q.adaptersToResults.length })
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
            }, _class.inject = [EventAggregator, QEPRepository, notifications], _temp));

            _export('QueriesModel', QueriesModel);
        }
    };
});
//# sourceMappingURL=queries.model.js.map
