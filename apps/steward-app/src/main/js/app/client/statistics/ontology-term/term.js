(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .directive('queryTerm', TermDirectiveFactory);

    function TermDirectiveFactory () {

        var templateUrl = './app/client/statistics/' +
            'ontology-term/term.tpl.html';
        var termDirective = {
            restrict: 'A',
            templateUrl: templateUrl,
            controller: TermController,
            require: '^termDigest'
        };

        return termDirective;
    }

    TermController.$inject = ['$scope', 'TermService'];
    function TermController ($scope, service) {
        $scope.svc = service;
    }
})();