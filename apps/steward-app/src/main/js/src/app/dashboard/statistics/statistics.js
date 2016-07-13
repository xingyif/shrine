'use strict';
/**
 * @ngdoc function
 * @name sbAdminApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the sbAdminApp
 */
angular.module('stewardApp')
    .controller('StatisticsCtrl', ['$scope', '$timeout', '$app', 'StatisticsModel', function ($scope, $timeout, $app, model) {



        //existing date range logic.
        var startDate = new Date(),
            endDate   = new Date();

        startDate.setDate(endDate.getDate() - 7);

        $scope.getDateString = function (date) {
            return $app.utils.utcToMMDDYYYY(date);
        };

        $scope.startDate        = $scope.getDateString(startDate);
        $scope.endDate          = $scope.getDateString(endDate);
        $scope.isValid          = true;
        $scope.startOpened      = false;
        $scope.endOpened        = false;
        $scope.queriesPerUser   = {};
        $scope.topicsPerState   = {};

        $scope.format = 'MM/dd/yyyy';
        //http://angular-ui.github.io/bootstrap/

        $scope.openStart = function ($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.startOpened = true;
        };

        $scope.openEnd = function ($event) {
            $event.preventDefault();
            $event.stopPropagation();
            $scope.endOpened = true;
        };

        $scope.validateRange = function () {

            var startUtc, endUtc, secondsPerDay = 86400000;

            if ($scope.startDate === undefined || $scope.endDate === undefined) {
                $scope.isValid = false;
                return;
            }

            //can validate date range here.
            startUtc = $app.utils.timestampToUtc($scope.startDate);
            endUtc   = $app.utils.timestampToUtc($scope.endDate) + secondsPerDay;

            if (endUtc - startUtc <= 0) {
                $scope.isValid = false;
            } else {
                $scope.isValid = true;
            }

            return $scope.isValid;
        };

        $scope.addDateRange = function () {

            if (!$scope.validateRange()) {
                return;
            }

            var secondsPerDay = 86400000;

            $scope.getResults($app.utils.timestampToUtc($scope.startDate), $app.utils.timestampToUtc($scope.endDate) + secondsPerDay);
        };

        //@todo: this is workaround logic.
        $scope.parseStateTitle = function (state) {

            var title = "";

            if (state.Approved !== undefined) {
                title =  "Approved";
            }

            else {
                title =  (state.Rejected !== undefined) ? "Rejected" : "Pending";
            }

            return title;
        };

        //@todo: this is workaround logic.
        $scope.parseStateCount = function (state) {
            var member = $scope.parseStateTitle(state);
            return state[member];
        };


        $scope.getResults =  function (startUtc, endUtc) {
            model.getQueriesPerUser(startUtc, endUtc)
                .then(function (result) {
                    $scope.queriesPerUser = result;
                });

            model.getTopicsPerState(startUtc, endUtc)
                .then(function (result) {
                    $scope.topicsPerState = result;
                });
        };

        $scope.addDateRange();
        // -- end existing statistics logic --//
    }])
    .directive("queryCounts", function () {
        return {
            restrict: "E",
            templateUrl: "app/client/dashboard/statistics/query-counts-table.tpl.html",
            replace: true
        };
    })
    .directive("topicStatus", function () {
        return {
            restrict: "E",
            templateUrl: "app/client/dashboard/statistics/query-topic-status-table.tpl.html",
            replace: true
        };
    });

