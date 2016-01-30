(function (){
    'use strict';

    // -- scope constant  -- //
    var URLS = {
        base: 'test/admin/status/summary.json'
    };

    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel);

    // -- minifaction proof injection -- //
    DiagnosticModel.$inject = ['$http', '$q'];
    function DiagnosticModel (h, q) {

        // -- public -- //
        return {
            getSummary: getSummary
        };

        /**
         *
         * @param failedResult
         * @returns {*}
         */
        function onFail(failedResult) {
            return failedResult;
        }

        /***
         *
         * @param result
         * @returns {*}
         */
        function parseResult(result) {
            return result.data;
        }

        /**
         *
         * @param verb
         * @returns {*}
         */
        function getSummary(verb) {
            var url = URLS.base;
            return h.get(url)
                .then(parseResult, onFail);
        }
    }

})();
