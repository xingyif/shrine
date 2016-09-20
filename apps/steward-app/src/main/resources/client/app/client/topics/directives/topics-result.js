(function () {
    'use strict';

    angular.module('shrine.steward.topics')
        .directive('topicsResult', TopicResultsDirective);

    function TopicResultsDirective() {

        var scope = {
            showSteward:'@',
            sortData: '=',
            sort: '&',
            dateFormatter: '&',
            topics: '=',
            open: '&',
            new: '&'
        };

        var topicResults =  {
            templateUrl: './app/client/topics/directives/topics-result.tpl.html',
            restrict: 'E',
            replace: true,
            controller: TopicsResultController,
            controllerAs: 'result',
            scope: scope
        };

        return topicResults;
    }

    TopicsResultController.$inject = ['$scope'];
    function TopicsResultController($scope) {
        var result = this;
        result.showSteward = $scope.showSteward;
        result.sortData = $scope.sortData;
        result.sort = $scope.sort;
        result.dateFormatter = $scope.dateFormatter;
        result.open = $scope.open;
        result.new = $scope.new;
    }
})();