angular.module("happy-model", ['shrine.common'])
    .service("HappyMdl", ['$http', '$q', 'UtilsService', '$app', function ($http, $q, mdlSvc, $app) {

        var URLS        = {
            ALL:        'all',
            KEYSTORE:   'keystore',
            VERSION:    'version',
            QUERIES:    'queries',
            ROUTING:    'routing',
            HIVE:       'hive',
            NETWORK:   'network',
            ADAPTER:    'adapter',
            AUDIT:      'audit'
        }, base = 'admin/happy/';


        function HappyMdl() {
            this.base  = base;
            this.model = null;
        }

        function onFail(result) {
            return result;
        }

        function parseResult(result) {
            if(isQEPError(result.data)) {
                return $q.reject(result.data);
            }

            return $app.utils.xmlToJson(result.data);
        }

        function hget(verb) {
            var url = mdlSvc.getUrl() + base + verb;
            return $http.get(url)
                .then(parseResult, onFail);
        }

        HappyMdl.prototype.getAll = function () {
           return hget(URLS.ALL);
        };

        HappyMdl.prototype.getKeystore = function () {
            return hget(URLS.KEYSTORE);
        };

        HappyMdl.prototype.getVersion = function () {
            return hget(URLS.VERSION);
        };

        HappyMdl.prototype.getQueries = function () {
            return hget(URLS.QUERIES);
        };

        HappyMdl.prototype.getRouting = function () {
            return hget(URLS.ROUTING);
        };

        HappyMdl.prototype.getHive = function () {
            return hget(URLS.HIVE);
        };

        HappyMdl.prototype.getNetwork = function () {
            return hget(URLS.NETWORK);
        };

        HappyMdl.prototype.getAdapter = function () {
            return hget(URLS.ADAPTER);
        };

        HappyMdl.prototype.getAudit = function () {
            return hget(URLS.AUDIT);
        };

        /**
         *
         * @param resultXML
         * @returns {boolean}
         */
        function isQEPError(resultXML) {
            var result = resultXML.indexOf('<all>') + resultXML.indexOf('</all>');
            return result == -2
        }

        return new HappyMdl();
    }]);

