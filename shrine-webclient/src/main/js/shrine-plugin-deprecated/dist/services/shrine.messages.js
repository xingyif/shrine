'use strict';

System.register([], function (_export, _context) {
  "use strict";

  var notifications, commands;
  return {
    setters: [],
    execute: function () {
      _export('notifications', notifications = {
        i2b2: {
          tabMax: 'notification.from.i2b2.tab.max',
          tabMin: 'notification.from.i2b2.tab.min',
          viewSelected: 'notification.from.i2b2.tab.selected',
          historyRefreshed: 'notification.from.i2b2.history.refreshed',
          queryStarted: 'notification.from.i2b2.query.started',
          messageReceived: 'notification.from.i2b2.message.received',
          networkIdReceived: 'notification.from.i2b2.networkId.receieved',
          exportQuery: 'notification.from.i2b2.export.query',
          clearQuery: 'notification.from.i2b2.clear.query'
        },

        shrine: {
          queriesReceived: 'notification.from.shrine.queries.received',
          queryReceived: 'notification.from.shrine.query.recieved',
          queryUnavailable: 'notification.from.shrine.query.unavailable',
          queryAvailable: 'notification.from.shrine.query.available',
          refreshAllHistory: 'notification.from.shrine.refresh.all.history'

        }
      });

      _export('notifications', notifications);

      _export('commands', commands = {
        i2b2: {
          cloneQuery: 'command.to.i2b2.clone.query',
          showError: 'command.to.i2b2.show.error',
          flagQuery: 'command.to.i2b2.flag.query',
          unflagQuery: 'command.to.i2b2.unflag.query',
          renameQuery: 'command.to.i2b2.rename.query'
        },

        shrine: {
          fetchQuery: 'command.to.shrine.fetch.query',
          exportResult: 'command.to.shrine.export.result'
        }
      });

      _export('commands', commands);
    }
  };
});
//# sourceMappingURL=shrine.messages.js.map
