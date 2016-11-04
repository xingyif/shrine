(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('termDigest', TermDigestDirective);

    function TermDigestDirective(OntologyTermService) {
        var templateUrl = './app/client/statistics/' +
            'ontology-term/term-digest.tpl.html';

        var termDigest = {
            restrict: 'E',
            templateUrl: templateUrl,
            controller: QueryDigestController,
            controllerAs: 'digest',
            scope: {
                ontology: '='
            },
            link: TermDigestLinker,
            transclude: true
        };

        return termDigest;
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

    function TermDigestLinker(scope) {
        scope.$watch('ontology', function (before, after) {
            var digest = scope.digest;
            digest.getOntologyByTopic();
        });
    }
})();