(function () {
    'use strict';

    angular
        .module('shrine.steward.statistics')
        .controller('StatisticsController', StatisticsController);

    StatisticsController.$inject = ['StatisticsModel', 'StewardService', '$scope'];
    function StatisticsController(model, service, $scope) {
        var showOntClass = 'ont-overlay';
        var hideOntClass = 'ont-hidden';
        var stats = this;
        var startDate = new Date();
        var endDate = new Date();

        // -- public vars --//
        stats.ontClass = hideOntClass;
        startDate.setDate(endDate.getDate() - 7);
        stats.getDateString = service.commonService.dateService.utcToMMDDYYYY;
        stats.timestampToUtc = service.commonService.dateService.timestampToUtc;
        stats.viewDigest = viewDigest;
        stats.startDate = startDate;
        stats.endDate = endDate;
        stats.isValid = true;
        stats.startOpened = false;
        stats.endOpened = false;
        stats.topicsPerState = {};
        stats.ontology = {};
        stats.graphData = {
            total: 0,
            users: []
        };
        stats.format = 'MM/dd/yyyy';

        // --  public methods --//
        stats.openStart = openStart;
        stats.openEnd = openEnd;
        stats.validateRange = validateRange;
        stats.addDateRange = addDateRange;
        stats.parseStateTitle = parseStateTitle;
        stats.parseStateCount = parseStateCount;
        stats.getResults = getResults;
        stats.viewDigest = viewDigest;
        stats.broadcastReset = broadcastReset;

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

        // -- @todo: code sloppy, refactor --//
        function validateRange() {

            var startUtc, endUtc;

            if (stats.startDate === undefined || stats.endDate === undefined) {
                stats.isValid = false;
                return;
            }

            //@todo: abstract to date methods. i.e. dateService.floor(date) and dateService.ceil(date)
            stats.startDate.setHours(0, 0, 0, 0);
            stats.endDate.setHours(23, 59, 59, 999);

            //@todo: abstract to reusable Date validate range method i.e dateService.validate(startDate, endDate).
            startUtc = stats.timestampToUtc(stats.startDate);
            endUtc = stats.timestampToUtc(stats.endDate);

            if (endUtc - startUtc <= 0) {
                stats.isValid = false;
            } else {
                stats.isValid = true;
            }

            return stats.isValid;
        }

        function addDateRange() {

            if (stats.validateRange()) {
                stats.getResults(stats.timestampToUtc(stats.startDate),
                    stats.timestampToUtc(stats.endDate));
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

        function viewDigest(data) {
            var startUtc = stats.timestampToUtc(stats.startDate);
            var endUtc = stats.timestampToUtc(stats.endDate);

            model.getUserQueryHistory(data.username.toLowerCase(), startUtc, endUtc)
                .then(function (result) {
                    stats.ontology = result.queryRecords;
                    stats.ontClass = showOntClass;
                });
        }

        function parseStateCount(state) {
            var member = stats.parseStateTitle(state);
            return state[member];
        }

        function getResults(startUtc, endUtc) {

            model.getQueriesPerUser(startUtc, endUtc)
                .then(function (result) {
                    stats.graphData = result;
                });

            model.getTopicsPerState(startUtc, endUtc)
                .then(function (result) {
                    stats.topicsPerState = result;
                });
        }

        function broadcastReset() {
            $scope.$broadcast('reset-digest');
        }
    }
})();
