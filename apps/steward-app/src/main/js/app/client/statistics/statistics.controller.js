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

        // -- setup --//
        //stats.startDate.setDate(endDate.getDate() - 7);


        stats.openStart = openStart;
        stats.openEnd = openEnd;
        stats.validateRange = validateRange;
        stats.addDateRange = addDateRange;
        stats.parseStateTitle = parseStateTitle;
        stats.parseStateCount = parseStateCount;
        stats.getResults = getResults;

        // -- private -- //
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

        //stats.addDateRange();



        ///////
/*        stats.startOpen = stats.endOpen = false;


        $scope.today = function () {
            $scope.dt = new Date();
        };
        $scope.today();

        $scope.clear = function () {
            $scope.dt = null;
        };

        $scope.inlineOptions = {
            customClass: getDayClass,
            minDate: new Date(),
            showWeeks: true
        };

        $scope.dateOptions = {
            dateDisabled: disabled,
            formatYear: 'yy',
            maxDate: stats.endDate,
            minDate: stats.startDate,
            startingDay: 1
        };

        // Disable weekend selection
        function disabled(data) {
            var date = data.date,
                mode = data.mode;
            return mode === 'day' && (date.getDay() === 0 || date.getDay() === 6);
        }

        $scope.toggleMin = function () {
            $scope.inlineOptions.minDate = $scope.inlineOptions.minDate ? null : new Date();
            $scope.dateOptions.minDate = $scope.inlineOptions.minDate;
        };

        $scope.toggleMin();

        /*stats.openStart = function () {
            stats.startPopup.opened = true;
        };*/

/*
        $scope.open1 = function () {
            $scope.popup1.opened = true;
        };

        $scope.open2 = function () {
            $scope.popup2.opened = true;
        };

        $scope.setDate = function (year, month, day) {
            $scope.dt = new Date(year, month, day);
        };

        ///$scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
        ///$scope.format = $scope.formats[0];
        ///$scope.altInputFormats = ['M!/d!/yyyy'];

        stats.startPopup = {
            opened: false
        }

        stats.endPopup = {
            opened: false
        }



        $scope.popup1 = {
            opened: false
        };

        $scope.popup2 = {
            opened: false
        };

        var tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        var afterTomorrow = new Date();
        afterTomorrow.setDate(tomorrow.getDate() + 1);
        $scope.events = [
            {
                date: tomorrow,
                status: 'full'
            },
            {
                date: afterTomorrow,
                status: 'partially'
            }
        ];

        function getDayClass(data) {
            var date = data.date,
                mode = data.mode;
            if (mode === 'day') {
                var dayToCheck = new Date(date).setHours(0, 0, 0, 0);

                for (var i = 0; i < $scope.events.length; i++) {
                    var currentDay = new Date($scope.events[i].date).setHours(0, 0, 0, 0);

                    if (dayToCheck === currentDay) {
                        return $scope.events[i].status;
                    }
                }
            }

            return '';
        }*/
    }
})();
