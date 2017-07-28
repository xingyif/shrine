System.register([], function (_export, _context) {
    "use strict";

    var QueryStatus;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [],
        execute: function () {
            _export('QueryStatus', QueryStatus = function QueryStatus() {
                _classCallCheck(this, QueryStatus);

                var query = {
                    nodeResults: [{
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa1',
                        statusMessage: 'FINISHED',
                        count: 1810,
                        breakdowns: []
                    }, {
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa2',
                        statusMessage: 'ERROR',
                        statusDescription: 'Error status 1: Could not map query term(s).',
                        count: 10,
                        breakdowns: []
                    }, {
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa3',
                        statusMessage: 'PROCESSING',
                        count: 10,
                        breakdowns: []
                    }, {
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa4',
                        statusMessage: 'UNAVAILABLE',
                        count: 10,
                        breakdowns: []
                    }, {
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa5',
                        statusMessage: 'FINISHED',
                        count: -1,
                        breakdowns: []
                    }, {
                        timestamp: 1490571987946,
                        adapterNode: 'shrine-qa6',
                        statusMessage: 'ERROR',
                        statusDescription: 'Error status 2: Could not map query term(s).',
                        count: 10,
                        breakdowns: []
                    }],
                    name: 'Female@12:00:35',
                    networkQueryId: 6386518509045377000,
                    completed: false
                };

                this.query = query;
            });

            _export('QueryStatus', QueryStatus);
        }
    };
});
//# sourceMappingURL=query-status.js.map
