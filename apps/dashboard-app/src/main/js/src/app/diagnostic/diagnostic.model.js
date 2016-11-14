(function (){
    'use strict';


    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('DiagnosticModel', DiagnosticModel);


    DiagnosticModel.$inject = ['$http', '$q', 'UrlGetter'];
    function DiagnosticModel (h, q, urlGetter) {

        var toDashboard = {url:''};
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
            SummaryEndpoint:  'admin/status/summary'
        };


        // -- public -- //
        return {
            getAdapter:        getJsonMaker(Config.AdapterEndpoint, 'adapter'),
            getConfig:         getJsonMaker(Config.ConfigEndpoint, 'config', parseConfig),
            getHub:            getJsonMaker(Config.HubEndpoint, 'hub'),
            getI2B2:           getJsonMaker(Config.I2B2Endpoint, 'i2b2'),
            getKeystore:       getJsonMaker(Config.KeystoreEndpoint, 'keystore'),
            getOptionalParts:  getJsonMaker(Config.OptionsEndpoint, 'optionalParts'),
            getProblems:       getProblemsMaker(),
            getQep:            getJsonMaker(Config.QepEndpoint, 'qep'),
            getSummary:        getJsonMaker(Config.SummaryEndpoint, 'summary'),
            map:               map,
            cache:             cache,
            toDashboard:       toDashboard
        };

        function map(func, list) {
            var result = [];
            for(var i = 0; i < list.length; i++) {
                result.push(func(list[i]))
            }
            return result;
        }


        /**
         * Method for Handling a failed rest call.
         * @param failedResult
         * @returns {*}
         */
        function onFail(failedResult) {
            return {failed: failedResult};
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
         * Parses the json config map and turns it into a nested json object
         * @param json the flat config map
         * @param cacheKey a unique identifier for the function
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

        // "explodes" and merges the flag config map.
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
         * There's a lot going on here. Essentially, this is a function factory that allows one to
         * define backend calls just through the path. It also implements a simple caching
         * strategy.
         * Essentially the get function only needs to be called once, and from then on it will spit
         * back a cached promise. This lets you write the code and not care whether it's cached or
         * not, but also get the caching performance anyways. For this function to work, the
         * resolver function has to take in the http response and the cache key to set, and make
         * sure that it caches what it returns (see parseJsonResult or parseConfig).
         * @param endpoint
         * @param cacheKey
         * @param resolverDefault
         * @returns {Function}
         */
        function getJsonMaker(endpoint, cacheKey, resolverDefault) {
            var resolver = (typeof resolverDefault !== 'undefined')?
                           function (response) { return resolverDefault(response, cacheKey) }:
                           function (response) { return parseJsonResult(response, cacheKey) };
            return function() {
                var cachedValue = cache[cacheKey];
                if (cachedValue === undefined) {
                    var url = urlGetter(endpoint, undefined, toDashboard.url);
                    return h.get(url)
                        .then(resolver, onFail)
                } else {
                    return q(function(resolver) { resolver(cachedValue)});
                }
            }
        }

        function getProblemsMaker() {
            // Caches the last offset and page size to hold onto it between different views
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
                    Config.ProblemEndpoint + '?offset=' + offset + '&n=' + n + epochString,
                    undefined,
                    toDashboard.url
                );
                return h.get(url)
                    .then(parseJsonResult, onFail);
            }
        }
    }
})();
