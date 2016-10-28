(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('termDigest', TermDigestDirective);

    function TermDigestDirective (OntologyTermService) {
        var templateUrl = './app/client/statistics/directives/' +
            'ontology-term/term-digest.tpl.html';

        var termDigest  = {
            restrict: 'E',
            templateUrl: templateUrl,
            controller: QueryDigestController,
            controllerAs: 'digest',
            scope: {
                ontology: '=',
                max: '@'
            },
            link: TermDigestLinker,
            transclude: true
        };

        return termDigest;
    }

    QueryDigestController.$inject = ['OntologyTermService'];
    function QueryDigestController (OntologyTermService) {
        var digest = this;
        digest.ontologyTermService =  OntologyTermService;
    }

    function TermDigestLinker(scope) {
        scope.$watch('ontology', function(before, after) {
            var digest = scope.digest;
            var service = digest.ontologyTermService;  
            digest.ontology = service.buildOntology(scope.ontology);
            digest.max = service.getMax();
        });
    }
})();