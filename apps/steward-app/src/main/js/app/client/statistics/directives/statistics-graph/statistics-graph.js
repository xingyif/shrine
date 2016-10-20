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
                graphData: '=',
                graphClick: '&' /** todo pass in click handler. **/
            }
        };

        return statisticsGraph;
    }

    StatisticsGraphController.$inject = ['$scope', 'StatisticsGraphService'];
    function StatisticsGraphController($scope, svc) {
        var graph = this;
        //graphService = svc;
        graph.graphData = $scope.graphData;
        graph.toPercentage = toPercentage;

        function toPercentage(value) {
            var maxQueryCount = svc.getMaxUserQueryCount(graph.graphData.users) || 1;
            return svc.getCountAsPercentage(value, maxQueryCount);
        }
    }

    StatisticsGraphLink.$inject = ['scope'];
    function StatisticsGraphLink(scope) {
        scope.$watch('graphData', function(before, after) {
            var graph = scope.graph;
            graph.graphData = scope.graphData;
        });
        var test = arguments;
    }
})();