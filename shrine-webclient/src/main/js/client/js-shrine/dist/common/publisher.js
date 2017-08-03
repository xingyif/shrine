System.register(['aurelia-framework', 'aurelia-event-aggregator', 'common/shrine.messages'], function (_export, _context) {
    "use strict";

    var inject, EventAggregator, commands, _dec, _class, Publisher;

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
        }, function (_commonShrineMessages) {
            commands = _commonShrineMessages.commands;
        }],
        execute: function () {
            _export('Publisher', Publisher = (_dec = inject(EventAggregator, commands), _dec(_class = function Publisher(evtAgg, commands) {
                _classCallCheck(this, Publisher);

                this.commands = commands;
                this.publish = function (c, d) {
                    return evtAgg.publish(c, d);
                };
            }) || _class));

            _export('Publisher', Publisher);
        }
    };
});
//# sourceMappingURL=publisher.js.map
