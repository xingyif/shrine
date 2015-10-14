angular.module("happy-model", ['model-service'])
    .service("HappyMdl", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {

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
            return $app.utils.xmlToJson(result.data);
        }

        function hget(verb) {
            var url = mdlSvc.url.base + base + verb;
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

        return new HappyMdl();
    }]);

