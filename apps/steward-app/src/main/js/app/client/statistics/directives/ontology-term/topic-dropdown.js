
(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('topicDropdown', TopicDropdownFactory);

    function TopicDropdownFactory() {

        var templateUrl = './app/client/statistics/directives/' +
            'ontology-term/topic-dropdown.tpl.html';

        var topicDropdown = {
            scope: {
                topics: '=',
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
    function TopicDropdownController ($scope, OntologyTermService) {
        var dropdown = this;
        dropdown.ontologyTermService = OntologyTermService;
        dropdown.topics = $scope.topics;
    }

    function TopicDropdownLinker (scope) {
        scope.$watch('topics', function(before, after) {
            var dropdown  = scope.dropdown;
            var service = dropdown.ontologyTermService;
            dropdown.topics = scope.topics;
        });
    }
})();
