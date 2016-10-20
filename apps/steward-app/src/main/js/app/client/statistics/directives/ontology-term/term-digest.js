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
            controllAs: 'digest',
            scope: {
                ontology: '='
            },
            link: TermDigestLinker
        };

        return termDigest;
    }

    function TermDigestLinker() {
    }
})();