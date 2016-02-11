(function (){
    'use strict';


    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel)


    DiagnosticModel.$inject = ['$http', '$q', 'UrlGetter'];
    function DiagnosticModel (h, q, urlGetter) {


        var cache = {};

        // -- private const -- //
        var Config = {
            OptionsEndpoint: 'admin/status/options',
            ConfigEndpoint:  'admin/status/config',
            SummaryEndpoint: 'admin/status/summary'
        };


        // -- public -- //
        return {
            getOptions: getOptions,
            getConfig:  getConfig,
            getSummary: getSummary,
            cache:      cache
        };

        /**
         * Method for Handling a failed rest call.
         * @param failedResult
         * @returns {*}
         */
        function onFail(failedResult) {
            return failedResult;
        }


        /***
         * Method for handling a successful rest call.
         * @param result
         * @returns {*}
         */
        function parseResult(result) {
            return result.data;
        }


        /**
         * Get View Options, initial call from diagnostic.
         * @param verb
         * @returns {*}
         */
        function getOptions() {
            var url = urlGetter(Config.OptionsEndpoint)
            return h.get(url)
                .then(parseResult, onFail);
        }


        /**
         * Returns the Shrine Configuration object.
         * @returns {*}
         */
        function getConfig () {
            var url = urlGetter(Config.ConfigEndpoint)
            return h.get(url)
                .then(parseResult, onFail);
        }

        function getSummary () {
            var url = urlGetter(Config.SummaryEndpoint)
            return h.get(url)
                .then(parseResult, onFail);
        }
    }
})();
