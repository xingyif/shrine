(function (){
    'use strict';


    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel)


    DiagnosticModel.$inject = ['$http', '$q', 'UrlGetter', 'XMLService', '$log'];
    function DiagnosticModel (h, q, urlGetter, xmlService, $log) {


        var cache = {};

        // -- private const -- //
        var Config = {
            AdapterEndpoint:  'admin/status/adapter',
            ConfigEndpoint:   'admin/status/config',
            HubEndpoint:      'admin/status/hub',
            I2B2Endpoint:     'admin/status/i2b2',
            KeystoreEndpoint: 'admin/status/keystore',
            OptionsEndpoint:  'admin/status/optionalParts',
            ProblemEndpoint:  'admin/status/problems',
            QepEndpoint:      'admin/status/qep',
            SummaryEndpoint:  'admin/status/summary',
            HappyAllEndpoint: 'admin/happy/all'
        };


        // -- public -- //
        return {
            getAdapter:  getJsonMaker(Config.AdapterEndpoint, 'adapter'),
            getConfig:   getJsonMaker(Config.ConfigEndpoint, 'config', parseConfig),
            getHub:      getJsonMaker(Config.HubEndpoint, 'hub'),
            getI2B2:     getJsonMaker(Config.I2B2Endpoint, 'i2b2'),
            getKeystore: getJsonMaker(Config.KeystoreEndpoint, 'keystore'),
            getOptions:  getJsonMaker(Config.OptionsEndpoint, 'options'),
            getProblems: getProblemsMaker(),
            getQep:      getJsonMaker(Config.QepEndpoint, 'qep'),
            getSummary:  getJsonMaker(Config.SummaryEndpoint, 'summary'),
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
         * @param cacheKey
         * @returns {*}
         */
        function parseJsonResult(result, cacheKey) {
            cache[cacheKey] = result.data;
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
            summary.ontologyVersion = all.versionInfo.ontologyVersion;
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
         * Parses the json config map and turns it into a nested json object
         * @param json the flat config map
         */
        function parseConfig (json, cacheKey) {
            var configMap = json.data.configMap;
            var processed =  preProcessJson(configMap);
            cache[cacheKey] = processed;
            return processed;
        }

        // IE11 doesn't support string includes
        function stringIncludes(haystack, needle) {
            var arr = haystack.split("");
            for (var i = 0; i < arr.length; i++) {
                if (arr[i] == needle) {
                    return true;
                }
            }
            return false;
        }

        // "explodes" the flag config map.
        // e.g., {"key.foo": 10, "key.baz": 5} -> {"key": {"foo": 10, "baz": 5}}
        function preProcessJson (object) {
            var result = {};
            for (var key in object) {
                if (object.hasOwnProperty(key)) {
                    if (!stringIncludes(key, ".")) {
                        result[key] = object[key]
                    } else {
                        var split = key.split(".");
                        var prev = result;
                        for (var i = 0; i < split.length; i++) {
                            var cur = split[i];
                            if (!(cur in prev)) {
                                prev[cur] = {}
                            }
                            if (i == split.length - 1) {
                                prev[cur] = object[key];
                            } else {
                                prev = prev[cur]
                            }
                        }
                    }
                }
            }
            return result;
        }



        /**
         * Get View Options, initial call from diagnostic.
         * @param verb
         * @returns {*}
         */
        function getOptions() {
            var url = urlGetter(Config.OptionsEndpoint);
            return h.get(url)
                .then(parseJsonResult, onFail);
        }


        /**
         * Returns the Shrine Configuration object.
         * @returns {*}
         */
        function getConfig () {
            var url = urlGetter(Config.ConfigEndpoint);
            return h.get(url)
                .then(parseConfig, onFail);
        }

        /**
         * There's a lot going on here. Essentially, this is a function factory that allows one to
         * define backend calls just through the path. It also implements a simple caching strategy.
         * Essentially the get function only needs to be called once, and from then on it will spit
         * back a cached promise. This lets you write the code and not care whether it's cached or not,
         * but also get the caching performance anyways. For this function to work, the resolver
         * function has to take in the http response and the cache key to set, and make sure
         * that it caches what it returns (see parseJsonResult or parseConfig).
         * @param endpoint
         * @param cacheKey
         * @param resolverDefault
         * @returns {Function}
         */
        function getJsonMaker(endpoint, cacheKey, resolverDefault) {
            var resolver = (typeof resolver !== 'undefined')?
                           function (response) { return resolver(response, cacheKey) }:
                           function (response) { return parseJsonResult(response, cacheKey); };
            return function() {
                var cachedValue = cache[cacheKey];
                if (cachedValue === undefined) {
                    var url = urlGetter(endpoint);
                    return h.get(url)
                        .then(resolver, onFail)
                } else {
                    return q(function(resolver) { resolver(cachedValue)});
                }
            }
        }

        function getProblemsMaker() {

            var prevOffset = 0;
            var prevN = 20;

            /**
             * ProblemEndpoint:  'admin/status/problems',
             * @returns {*}
             */
            return function(offset, n, epoch) {
                if (offset != null) {
                    prevOffset = offset;
                } else {
                    offset = prevOffset;
                }
                if (n != null) {
                    prevN = n;
                } else {
                    n = prevN;
                }

                var epochString = epoch && isFinite(epoch) ? '&epoch=' + epoch : '';
                var url = urlGetter(
                    Config.ProblemEndpoint + '?offset=' + offset + '&n=' + n + epochString);
                return h.get(url)
                    .then(parseJsonResult, onFail);
            }
        }


        /**
         *
         * @returns {*}
         */
        function getHappyAll() {
            var url = urlGetter(Config.HappyAllEndpoint, '.xml');
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
