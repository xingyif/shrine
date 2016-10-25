(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('termDigest', TermDigestDirective);

            function TermDigestDirective () {
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
            link: TermDigestLinker
        };

        return termDigest;
    }

    function QueryDigestController () {
        var digest = this;
    }

    function TermDigestLinker(scope) {
        scope.$watch('ontology', function(before, after) {
            var digest = scope.digest;
            digest.ontology = scope.ontology;
            digest.max = scope.max;
        });
    }
})();