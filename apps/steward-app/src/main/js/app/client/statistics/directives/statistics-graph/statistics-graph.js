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
            scope: {
            }
        };

        return statisticsGraph;
    }


    StatisticsGraphController.$inject = ['StatisticsGraphService'];
    function StatisticsGraphController(statsService) {
    }
})();