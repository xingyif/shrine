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


        $scope.CountsByUserLine = {
            labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
            series: ['Series A', 'Series B'],
            data: [
                [65, 59, 80, 81, 56, 55, 40],
                [28, 48, 40, 19, 86, 27, 90]
            ],
            onClick: function (points, evt) {
                console.log(points, evt);
            }
        };

        $scope.line = {
            labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
            series: ['Series A', 'Series B'],
            data: [
                [65, 59, 80, 81, 56, 55, 40],
                [28, 48, 40, 19, 86, 27, 90]
            ],
            onClick: function (points, evt) {
                console.log(points, evt);
            }
        };

        $scope.bar = {
            labels: ['2006', '2007', '2008', '2009', '2010', '2011', '2012'],
            series: ['Series A', 'Series B'],

            data: [
                [65, 59, 80, 81, 56, 55, 40],
                [28, 48, 40, 19, 86, 27, 90]
            ]

        };

        $scope.donut = {
            labels: ["Download Sales", "In-Store Sales", "Mail-Order Sales"],
            data: [300, 500, 100]
        };

        $scope.radar = {
            labels:["Eating", "Drinking", "Sleeping", "Designing", "Coding", "Cycling", "Running"],

            data:[
                [65, 59, 90, 81, 56, 55, 40],
                [28, 48, 40, 19, 96, 27, 100]
            ]
        };

        $scope.pie = {
            labels : ["Download Sales", "In-Store Sales", "Mail-Order Sales"],
            data : [300, 500, 100]
        };

        $scope.polar = {
            labels : ["Download Sales", "In-Store Sales", "Mail-Order Sales", "Tele Sales", "Corporate Sales"],
            data : [300, 500, 100, 40, 120]
        };

        $scope.dynamic = {
            labels : ["Download Sales", "In-Store Sales", "Mail-Order Sales", "Tele Sales", "Corporate Sales"],
            data : [300, 500, 100, 40, 120],
            type : 'PolarArea',

            toggle : function ()
            {
                this.type = this.type === 'PolarArea' ?
                    'Pie' : 'PolarArea';
            }
        };
    }])
    .directive("queryCounts", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/statistics/query-counts-table.tpl.html",
            replace: true
        };
    })
    .directive("topicStatus", function () {
        return {
            restrict: "E",
            templateUrl: "src/app/dashboard/statistics/query-topic-status-table.tpl.html",
            replace: true
        };
    });

