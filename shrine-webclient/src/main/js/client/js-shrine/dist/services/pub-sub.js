'use strict';

System.register(['aurelia-event-aggregator', './shrine.messages'], function (_export, _context) {
    "use strict";

    var EventAggregator, commands, notifications, _class, _temp, PubSub;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaEventAggregator) {
            EventAggregator = _aureliaEventAggregator.EventAggregator;
        }, function (_shrineMessages) {
            commands = _shrineMessages.commands;
            notifications = _shrineMessages.notifications;
        }],
        execute: function () {
            _export('PubSub', PubSub = (_temp = _class = function PubSub(evtAgg, commands, notifications) {
                _classCallCheck(this, PubSub);

                this.commands = commands;
                this.notifications = notifications;
                this.publish = function (c, d) {
                    return evtAgg.publish(c, d);
                };
                this.subscribe = function (n, fn) {
                    return evtAgg.subscribe(n, fn);
                };
            }, _class.inject = [EventAggregator, commands, notifications], _temp));

            _export('PubSub', PubSub);
        }
    };
});
//# sourceMappingURL=pub-sub.js.map
