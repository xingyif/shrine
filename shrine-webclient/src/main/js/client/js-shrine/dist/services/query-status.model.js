System.register(['aurelia-event-aggregator', 'repository/qep.repository', './shrine.messages'], function (_export, _context) {
    "use strict";

    var EventAggregator, QEPRepository, commands, notifications, _extends, _class, _temp, QueryStatusModel;

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
            commands = _shrineMessages.commands;
            notifications = _shrineMessages.notifications;
        }],
        execute: function () {
            _extends = Object.assign || function (target) {
                for (var i = 1; i < arguments.length; i++) {
                    var source = arguments[i];

                    for (var key in source) {
                        if (Object.prototype.hasOwnProperty.call(source, key)) {
                            target[key] = source[key];
                        }
                    }
                }

                return target;
            };

            _export('QueryStatusModel', QueryStatusModel = (_temp = _class = function QueryStatusModel(evtAgg, qep, notifications) {
                _classCallCheck(this, QueryStatusModel);

                var publishNetworkId = function publishNetworkId(id) {
                    return evtAgg.publish(notifications.i2b2.networkIdReceived, id);
                };
                var publishQuery = function publishQuery(model) {
                    return evtAgg.publish(notifications.shrine.queryReceived, model);
                };
                var logError = function logError(error) {
                    return console.log('ERROR: ' + error);
                };
                var toModel = function toModel(data) {
                    return new Promise(function (resolve, reject) {
                        var nodes = [].concat(data.results);
                        var finishedCount = nodes.reduce(function (s, r) {
                            return ['FINISHED', 'ERROR'].indexOf(r.status) != -1 ? s + 1 : s;
                        }, 0);
                        var query = _extends({}, data.query, { complete: nodes.length > 0 && nodes.length === finishedCount });
                        resolve({
                            query: query,
                            nodes: nodes,
                            finishedCount: finishedCount
                        });
                    });
                };

                var loadNetworkId = function loadNetworkId(n) {
                    return qep.fetchNetworkId(n).then(function (result) {
                        return publishNetworkId(result);
                    }).catch(function (error) {
                        return logError(error);
                    });
                };

                var loadQuery = function loadQuery(id) {
                    return qep.fetchQuery(id).then(function (result) {
                        return toModel(result);
                    }).catch(function (error) {
                        return logError(error);
                    }).then(function (model) {
                        return publishQuery(model);
                    });
                };

                var init = function init() {
                    evtAgg.subscribe(commands.shrine.fetchQuery, function (id) {
                        return loadQuery(id);
                    });
                };
                init();
            }, _class.inject = [EventAggregator, QEPRepository, notifications], _temp));

            _export('QueryStatusModel', QueryStatusModel);
        }
    };
});
//# sourceMappingURL=query-status.model.js.map
