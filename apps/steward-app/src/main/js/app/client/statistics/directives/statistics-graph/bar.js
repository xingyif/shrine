(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('graphBar', GraphBarDirective);

    function GraphBarDirective() {
        var templateUrl = './app/client/statistics/directives/statistics-graph/' +
            'bar.tpl.html';

        var graphBar = {
            restrict: 'E',
            templateUrl: templateUrl,
            controller: GraphBarController,
            controllerAs: 'bar',
            scope: {
                username: '@',
                value: '@'
            }
        };

        return graphBar;
    }

    GraphBarController.$inject = ['$scope'];
    function GraphBarController($scope) {
        var bar = this;
        bar.username = $scope.username;
        bar.value = $scope.value;
    }
})();