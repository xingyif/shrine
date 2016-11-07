(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('queryDigest', QueryDigestDirective);

    function QueryDigestDirective(OntologyTermService) {
        var templateUrl = './app/client/statistics/' +
            'query-digest/query-digest.tpl.html';

        var queryDigest = {
            restrict: 'E',
            templateUrl: templateUrl,
            controller: QueryDigestController,
            controllerAs: 'digest',
            scope: {
                ontology: '='
            },
            link: QueryDigestLinker,
            transclude: true
        };

        return queryDigest;
    }

    QueryDigestController.$inject = ['$scope', 'OntologyTermService'];
    function QueryDigestController($scope, OntologyTermService) {
        var digest = this;
        //digest.ontologyTermService = OntologyTermService;

        var cache = {};

        this.getOntologyByTopic = getOntologyByTopic;
        function getOntologyByTopic(topicId) {

            var defaultData = {
                ontology: {},
                max: 0
            };

            var digest = $scope.digest;

            if (!$scope.ontology.length) {
                digest.ontology = defaultData;
            }

            else {
                digest.ontology = OntologyTermService.buildOntology($scope.ontology, topicId);
                digest.totalQueries = digest.ontology.queryCount;
                digest.max = OntologyTermService.getMax();
            }
        }
    }

    function QueryDigestLinker(scope) {
        scope.$watch('ontology', function (before, after) {
            var digest = scope.digest;
            digest.getOntologyByTopic();
        });
    }
})();