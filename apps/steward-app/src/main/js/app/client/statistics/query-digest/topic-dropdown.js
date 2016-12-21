
(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('topicDropdown', TopicDropdownFactory);

    function TopicDropdownFactory() {

        var templateUrl = './app/client/statistics/' +
            'query-digest/topic-dropdown.tpl.html';

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
        dropdown.reset = reset;

        /**
         * Reset handler.
         */
        function reset(events, data) {
            dropdown.selected = {
                title: 'All'
            };
        }
    }

    function TopicDropdownLinker(scope) {
        var clearTopicsWatch = scope.$watch('topics', function (after, before) {
            var dropdown = scope.dropdown;

            // -- listen for reset --//
            scope.$on('reset-digest', dropdown.reset);

            if (after && after.length) {
                dropdown.topics = after;
            }
        });
    }
})();
