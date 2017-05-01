(function () {

    angular.module('shrine.steward.statistics')
        .directive('statisticsGraph', StatisticsGraphDirective);



    function StatisticsGraphDirective() {
        var templateUrl = './app/client/statistics/statistics-graph/' +
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
        graph.graphData = $scope.graphData;
        graph.toPercentage = toPercentage;
        graph.formatuserName = formatuserName;
        graph.graphClick = $scope.graphClick;
        graph.clearUsers = svc.clearUsers;
        graph.formatuserName = formatuserName;

        function toPercentage(value) {
            var maxQueryCount = svc.getMaxUserQueryCount(graph.graphData.users) || 1;
            return svc.getCountAsPercentage(value, maxQueryCount);
        }

        function formatuserName(userName) {
            return svc.formatuserName(userName);
        }
    }

    StatisticsGraphLink.$inject = ['scope'];
    function StatisticsGraphLink(scope) {
        scope.$watch('graphData', function (before, after) {
            var graph = scope.graph;
            graph.graphData = scope.graphData;
            graph.clearUsers();
        });
        var test = arguments;
    }
})();