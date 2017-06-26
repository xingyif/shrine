System.register(['aurelia-framework', 'aurelia-event-aggregator', 'common/i2b2.pub-sub', 'common/queries.model', 'common/shrine.messages'], function (_export, _context) {
  "use strict";

  var inject, EventAggregator, I2B2PubSub, QueriesModel, notifications, _dec, _class, Shell;

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
    }, function (_commonI2b2PubSub) {
      I2B2PubSub = _commonI2b2PubSub.I2B2PubSub;
    }, function (_commonQueriesModel) {
      QueriesModel = _commonQueriesModel.QueriesModel;
    }, function (_commonShrineMessages) {
      notifications = _commonShrineMessages.notifications;
    }],
    execute: function () {
      _export('Shell', Shell = (_dec = inject(EventAggregator, I2B2PubSub, QueriesModel, notifications), _dec(_class = function Shell(evtAgg, i2b2PubSub, queries, notifications) {
        _classCallCheck(this, Shell);

        i2b2PubSub.listen();
        queries.load();
      }) || _class));

      _export('Shell', Shell);
    }
  };
});
//# sourceMappingURL=shell.js.map
