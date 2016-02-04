(function (){
    'use strict';


    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel)

    // -- minifaction proof injection -- //
    DiagnosticModel.$inject = ['$http', '$q', 'UrlGetter'];
    function DiagnosticModel (h, q, urlGetter) {

        var getUrl = {};

        // -- private const -- //
        var Config = {
            OptionsEndpoint: 'admin/status/options',
            ConfigEndpoint:  'admin/status/config'
        };


        // -- public -- //
        return {
            getOptions: getOptions
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
        function getOptions() {
            var url = urlGetter(Config.OptionsEndpoint)
            return h.get(url)
                .then(parseResult, onFail);
        }
    }

})();
