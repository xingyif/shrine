(function () {

    angular.module('shrine.steward.statistics')
        .directive('statisticsGraph', StatisticsGraphDirective);



    function StatisticsGraphDirective() {
        var templateUrl = './app/client/statistics/directives/statistics-graph/' +
            'statistics-graph.tpl.html';
        var statisticsGraph = {
            restrict: 'E',
            templateUrl: templateUrl,
            controller: StatisticsGraphController,
            controllerAs: 'graph',
            link: StatisticsGraphLink,
            scope: {
                graphData: '='
            }
        };

        return statisticsGraph;
    }


    StatisticsGraphController.$inject = ['$scope', 'StatisticsGraphService'];
    function StatisticsGraphController($scope, statsService) {
        var graph = this;
        graph.graphData = $scope.graphData;
    }

    function StatisticsGraphLink(scope) {
        scope.$watch('graphData', function(before, after){
            var graph = scope.graph;
            graph.graphData = scope.graphData;
        });
    }
})();