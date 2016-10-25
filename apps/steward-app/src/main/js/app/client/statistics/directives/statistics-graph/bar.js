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
            link: GraphBarLinker,
            controllerAs: 'bar',
            scope: {
                username: '@',
                value: '@',
                percentage: '@'
            }
        };

        return graphBar;
    }

    GraphBarController.$inject = ['$scope'];
    function GraphBarController($scope) {
        var bar = this;
    }

    function GraphBarLinker(scope) {
        var test = arguments;
    }
})();