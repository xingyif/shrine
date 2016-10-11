(function () {
    'use strict';

    angular
        .module('shrine.steward.statistics')
        .controller('StatisticsController', StatisticsController);

    StatisticsController.$inject = ['StatisticsService'];
    function StatisticsController(service) {

        var stats = this;
        var startDate = new Date();
        var endDate = new Date();
        startDate.setDate(endDate.getDate() - 7);

        stats.startDate = startDate;
        stats.endDate = endDate;

        stats.isValid = true;
        stats.startOpened = false;
        stats.endOpened = false;
        stats.queriesPerUser = {};
        stats.topicsPerState = {};
        stats.format = 'MM/dd/yyyy';

        stats.openStart = openStart;
        stats.openEnd = openEnd;
        stats.validateRange = validateRange;
        stats.addDateRange = addDateRange;
        stats.parseStateTitle = parseStateTitle;
        stats.parseStateCount = parseStateCount;

        // -- start -- //
        init();

        // -- private -- //
        function init() {
            addDateRange();
        }

        function openStart() {
            stats.startOpened = true;
        }

        function openEnd() {
            stats.endOpened = true;
        }

        function validateRange() {

            stats.isValid = service.validateRange(stats.startDate, stats.endDate);
            return stats.isValid;
        }

        function addDateRange() {

            if (stats.validateRange(stats.startDate, stats.endDate)) {
                service.getResults(setQueriesPerUser, setTopicsPerState);
            }
        }

        function setQueriesPerUser(result) {
            stats.queriesPerUser = result;
        }

        function setTopicsPerState(result) {
            stats.topicsPerState = result;
        }

        function parseStateTitle(state) {

            var title = '';

            if (state.Approved !== undefined) {
                title = 'Approved';
            }

            else {
                title = (state.Rejected !== undefined) ? 'Rejected' : 'Pending';
            }

            return title;
        }

        function parseStateCount(state) {
            var member = stats.parseStateTitle(state);
            return state[member];
        }
    }
})();
