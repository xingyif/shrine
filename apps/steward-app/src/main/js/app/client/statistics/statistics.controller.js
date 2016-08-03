(function () {
    'use strict';

    angular
        .module('shrine.steward.statistics')
        .controller('StatisticsController', StatisticsController);

    StatisticsController.$inject = ['StatisticsModel', 'StewardService', '$scope'];
    function StatisticsController(model, service, $scope) {

        var stats = this;
        var startDate = new Date();
        var endDate = new Date();


        stats.getDateString = service.commonService.dateService.utcToMMDDYYYY;
        stats.timestampToUtc = service.commonService.dateService.timestampToUtc;

        stats.startDate = startDate; //stats.getDateString(startDate);
        stats.endDate = endDate; //stats.getDateString(endDate);
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
        stats.getResults = getResults;

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

            var startUtc, endUtc;
            var secondsPerDay = 86400000;

            if (stats.startDate === undefined || stats.endDate === undefined) {
                stats.isValid = false;
                return;
            }

            //can validate date range here.
            startUtc = stats.timestampToUtc(stats.startDate);
            endUtc = stats.timestampToUtc(stats.endDate) + secondsPerDay;

             if (endUtc - startUtc <= 0) {
                stats.isValid = false;
            } else {
                stats.isValid = true;
            }

            return stats.isValid;
        }

        function addDateRange() {

            if (stats.validateRange()) {
                var secondsPerDay = 86400000;
                stats.getResults(stats.timestampToUtc(stats.startDate),
                    stats.timestampToUtc(stats.endDate) + secondsPerDay);
            }
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

        function getResults(startUtc, endUtc) {
            model.getQueriesPerUser(startUtc, endUtc)
                .then(function (result) {
                    stats.queriesPerUser = result;
                });

            model.getTopicsPerState(startUtc, endUtc)
                .then(function (result) {
                    stats.topicsPerState = result;
                });
        }
    }
})();
