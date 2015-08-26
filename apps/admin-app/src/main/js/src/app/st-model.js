angular.module("st-model", ['model-service'])
    .service("STMdl", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {
        var URLS        = {
                LOAD_CONFIG: "config/steward.config.json"
            };


        function STMdl() {
            this.model = null;
        }

        function onFail(result) {
            this.model = result;
            this.model.message  = "Get App Config Failed: ";
            return this;
        }

        function parseConfig(result) {
            this.model = result.data;
            return result;
        }

        STMdl.prototype.getConfig = function () {
            return $http.get(URLS.LOAD_CONFIG)
                .then(parseConfig, onFail);
        };

        return new STMdl();
    }]);
