(function () {

    angular.module('shrine.steward.statistics')
        .directive('statisticsGraph', StatisticsGraphDirective);



    function StatisticsGraphDirective() {
        
        var templateUrl = './app/client/statistics/directives/' +
            'statistics-graph/statistics-graph.tpl.html';
        var statisticsGraph = {
            restrict: 'E',
            templateUrl: templateUrl
        };

        return statisticsGraph;
    }

/*
    StatisticsGraphController.$inject = ['StatisticsGraphService'];
    function StatisticsGraphController(statsService) {

    }
*/
})();