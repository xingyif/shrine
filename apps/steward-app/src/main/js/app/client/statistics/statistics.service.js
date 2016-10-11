(function () {
    'use strict';

    angular
        .module('shrine.steward.statistics')
        .service('StatisticsService', StatisticsService);

    StatisticsService.$inject = ['StewardService', 'StatisticsModel']
    function StatisticsService(service, model) {

        var secondsPerDay = 86400000;
        var startUtc, endUtc;

        return {
            validateRange: validateRange,
            setDateRangeFromStrings: setDateRangeFromStrings,
            getResults: getResults
        };



        function setDateRangeFromStrings(dateStr1, dateStr2) {
            //can validate date range here.
            startUtc = service.commonService.dateService.timestampToUtc(dateStr1);
            endUtc = service.commonService.dateService.timestampToUtc(dateStr2) + secondsPerDay;
        }


        /**
         * Given two date strings, make sure start date is earlier than end date. 
         */
        function validateRange(startDateStr, endDateStr) {

            if (startDateStr === undefined || endDateStr === undefined) {
                return false;
            }

            setDateRangeFromStrings(startDateStr, endDateStr);

            if (endUtc - startUtc <= 0) {
                return false;
            } else {
                return true;
            }
        }

        function getResults(then1, then2) {

            model.getQueriesPerUser(startUtc, endUtc)
                .then(then1);

            model.getTopicsPerState(startUtc, endUtc)
                .then(then2);
        }
    }
})();
