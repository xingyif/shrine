angular.module("app-model", ['model-service'])
    .service("AppMdl", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {
        var URLS        = {
                LOAD_CONFIG: "config/steward.config.json"
            };


        function AppMdl() {

        }

        function onFail(result) {
            alert("Get App Config Failed: " + result);
        }

        function parseConfig(result) {
            return result.data;
        }

        AppMdl.prototype.getConfig = function () {
            return $http.get(URLS.LOAD_CONFIG)
                .then(parseConfig, onFail);
        };

        return new AppMdl();
    }]);
