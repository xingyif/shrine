(function (){
    'use strict';

    // -- scope constant  -- //
    var URLS = {
        base: 'test/admin/status/summary.json'
    };

    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('SidebarModel', SidebarModel);

    // -- minifaction proof injection -- //
    SidebarModel.$inject = ['$http', '$q', '$app'];
    function SidebarModel (h, q, a) {

        // -- public -- //
        return {
            getSummary: get
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
        function get(verb) {
            var url = URLS.base;
            return h.get(url)
                .then(parseResult, onFail);
        }
    }

})();