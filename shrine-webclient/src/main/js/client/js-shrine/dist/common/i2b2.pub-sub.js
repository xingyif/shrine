System.register(['aurelia-framework', 'aurelia-event-aggregator', './i2b2.service', './shrine.messages'], function (_export, _context) {
    "use strict";

    var inject, EventAggregator, I2B2Service, notifications, commands, _dec, _class, I2B2PubSub;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_i2b2Service) {
            I2B2Service = _i2b2Service.I2B2Service;
        }, function (_shrineMessages) {
            notifications = _shrineMessages.notifications;
            commands = _shrineMessages.commands;
        }],
        execute: function () {
            _export('I2B2PubSub', I2B2PubSub = (_dec = inject(EventAggregator, I2B2Service, notifications, commands), _dec(_class = function I2B2PubSub(evtAgg, i2b2Svc, notifications, commands) {
                _classCallCheck(this, I2B2PubSub);

                this.listen = function () {
                    i2b2Svc.onResize(function (a, b) {
                        return b.find(function (e) {
                            return e.action === 'ADD';
                        }) ? notifyTabMax() : notifyTabMin();
                    });
                    i2b2Svc.onHistory(function () {
                        return notifyHistoryRefreshed();
                    });
                    i2b2Svc.onQuery(function (e, d) {
                        return notifyQueryStarted(d[0].name);
                    });
                    i2b2Svc.onViewSelected(function (e) {
                        return notifyViewSelected(e.data);
                    });

                    evtAgg.subscribe(commands.i2b2.cloneQuery, commandCloneQuery);
                    evtAgg.subscribe(commands.i2b2.showError, commandShowError);
                    evtAgg.subscribe(commands.i2b2.renameQuery, commandRenameQuery);
                    evtAgg.subscribe(commands.i2b2.flagQuery, commandFlagQuery);
                    evtAgg.subscribe(commands.i2b2.unflagQuery, commandUnflagQuery);
                };

                var notifyTabMax = function notifyTabMax() {
                    return evtAgg.publish(notifications.i2b2.tabMax);
                };
                var notifyTabMin = function notifyTabMin() {
                    return evtAgg.publish(notifications.i2b2.tabMin);
                };
                var notifyHistoryRefreshed = function notifyHistoryRefreshed() {
                    return evtAgg.publish(notifications.i2b2.historyRefreshed);
                };
                var notifyQueryStarted = function notifyQueryStarted(n) {
                    return evtAgg.publish(notifications.i2b2.queryStarted, n);
                };
                var notifyViewSelected = function notifyViewSelected(v) {
                    return evtAgg.publish(notifications.i2b2.viewSelected, v);
                };

                var commandCloneQuery = function commandCloneQuery(d) {
                    return i2b2Svc.loadQuery(d);
                };
                var commandShowError = function commandShowError(d) {
                    console.log(commands.i2b2.showError + ':  ' + d);
                    i2b2Svc.errorDetail(d);
                };
                var commandRenameQuery = function commandRenameQuery(d) {
                    return i2b2Svc.renameQuery(d);
                };
                var commandFlagQuery = function commandFlagQuery(d) {
                    return i2b2Svc.flagQuery(d);
                };
                var commandUnflagQuery = function commandUnflagQuery(d) {
                    return i2b2Svc.unflagQuery(d);
                };
            }) || _class));

            _export('I2B2PubSub', I2B2PubSub);
        }
    };
});
//# sourceMappingURL=i2b2.pub-sub.js.map
