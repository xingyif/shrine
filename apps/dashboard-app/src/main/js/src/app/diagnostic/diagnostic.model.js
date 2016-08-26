(function (){
    'use strict';


    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel)


    DiagnosticModel.$inject = ['$http', '$q', 'UrlGetter', 'XMLService'];
    function DiagnosticModel (h, q, urlGetter, xmlService) {


        var cache = {};

        // -- private const -- //
        var Config = {
            OptionsEndpoint:  'admin/status/options',
            ConfigEndpoint:   'admin/status/config',
            SummaryEndpoint:  'admin/status/summary',
            ProblemEndpoint:  'admin/status/problems',
            HappyAllEndpoint: 'admin/happy/all'
        };


        // -- public -- //
        return {
            getOptions:  getOptions,
            getConfig:   getConfig,
            getSummary:  getSummary,
            getProblems: getProblems,
            getHappyAll: getHappyAll,
            cache:       cache
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
        function parseJsonResult(result) {
            return result.data;
        }


        /**
         *
         * @param result
         * @returns {*}
         */
        function parseHappyAllResult(result) {

            var happyObj = {};
            if(isQEPError(result.data)) {
                return $q.reject(result.data);
            }

            // -- append all -- //
            happyObj.all = xmlService.xmlStringToJson(result.data).all;

            // -- parse and append summary  -- //
            happyObj.summary = parseSummaryFromAll(happyObj.all);

            return happyObj;
        }


        /**
         *
         * @param all
         * @returns {{}}
         */
        function parseSummaryFromAll (all) {

            //
            var summary             = {};

            summary.isHub           = !Boolean("" == all.notAHub);
            summary.shrineVersion   = all.versionInfo.shrineVersion;
            summary.shrineBuildDate = all.versionInfo.buildDate;
            summary.ontologyVersion = all.versionInfo.ontologyVersion
            summary.ontologyTerm    = ""; //to be implemented in config.
            summary.adapterOk       = all.adapter.result.response.errorResponse === undefined;
            summary.keystoreOk      = true;
            summary.qepOk           = true;

            // -- verify hub is operating, if necessary -- //
            if(!summary.isHub) {
                summary.hubOk = true;
            }
            else if(all.net !== undefined) {
                var hasFailures         = Number(all.net.failureCount) > 0;
                var hasInvalidResults   = Number(all.net.validResultCount) !=
                    Number(all.net.expectedResultCount);

                var hasTimeouts         = Number(all.net.timeoutCount) > 0;
                summary.hubOk           = !hasFailures && !hasInvalidResults && !hasTimeouts;
            }

            return summary;
        }


        /**
         * Get View Options, initial call from diagnostic.
         * @param verb
         * @returns {*}
         */
        function getOptions() {
            var url = urlGetter(Config.OptionsEndpoint)
            return h.get(url)
                .then(parseJsonResult, onFail);
        }


        /**
         * Returns the Shrine Configuration object.
         * @returns {*}
         */
        function getConfig () {
            var url = urlGetter(Config.ConfigEndpoint)
            return h.get(url)
                .then(parseJsonResult, onFail);
        }


        /**
         *
         * @returns {*}
         */
        function getSummary () {
            var url = urlGetter(Config.SummaryEndpoint)
            return h.get(url)
                .then(parseJsonResult, onFail);
        }

        /**
         * //todo
         * ProblemEndpoint:  'admin/status/problems',
         * @returns {*}
         */
        function getProblems(offset, n, epoch) {
            var url = urlGetter(Config.ProblemEndpoint+'?offset='+offset+'&n='+n+'&epoch='+epoch);
            return h.get(url)
                .then(parseJsonResult, onFail);
        }


        /**
         *
         * @returns {*}
         */
        function getHappyAll() {
            var url = urlGetter(Config.HappyAllEndpoint, '.xml')
            return h.get(url)
                .then(parseHappyAllResult, onFail);
        }

        /**
         *
         * @param resultXML
         * @returns {boolean}
         */
        function isQEPError(resultXML) {
            var result = resultXML.indexOf('<all>') + resultXML.indexOf('</all>');
            return result == -2
        }
    }
})();
