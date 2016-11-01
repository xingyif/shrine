
(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('topicDropdown', TopicDropdownFactory);

    function TopicDropdownFactory() {

        var templateUrl = './app/client/statistics/' +
            'ontology-term/topic-dropdown.tpl.html';

        var topicDropdown = {
            scope: {
                topics: '=',
                topicSelected: '&'
            },
            restrict: 'E',
            controller: TopicDropdownController,
            link: TopicDropdownLinker,
            controllerAs: 'dropdown',
            templateUrl: templateUrl
        };

        return topicDropdown;
    }

    TopicDropdownController.$inject = ['$scope', 'OntologyTermService'];
    function TopicDropdownController($scope, OntologyTermService) {
        var dropdown = this;
        dropdown.ontologyTermService = OntologyTermService;
        dropdown.topics = $scope.topics;
    }

    function TopicDropdownLinker(scope) {
        var clearWatch = scope.$watch('topics', function (after, before) {
            var dropdown = scope.dropdown;
            if (after && after.length) {
                dropdown.topics = after;
                clearWatch();
            }
        });
    }
})();
