angular
    .module("statistics-model", ['model-service'])
    .service("StatisticsModel", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {
        var queryResult      = {
                queriesPerUser: {
                    total: 0,
                    users: []
                },
                topicsPerState: {
                    total:  0,
                    states: []
                }
            },
            URLS        = {
                QUERIES_PER_USER: "/statistics/queriesPerUser",
                TOPICS_PER_STATE: "/statistics/topicsPerState"
            };

        function onFail(result) {
            alert("HTTP Request Fail: " + result);
        }

        function cacheQueriesPerUser(result) {

            var total = result.data.total,
                users = result.data.queriesPerUser;

            return {
                total: total,
                users: users
            };
        }

        function cacheTopicsPerState(result) {

            var total  = result.data.total,
                states = result.data.topicsPerState;

            return {
                total:  total,
                states: states
            };
        }

        function StatisticsMdl() {
            this.role = $app.globals.UserRoles.ROLE2;
        }

        /**
         * @todo: investigate sorting. see undefined params.
         */
        this.getQueriesPerUser = function (startDate, endDate) {
            var roleSegment = mdlSvc.getRoleSegment(this.role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base + roleSegment + URLS.QUERIES_PER_USER, undefined, undefined, undefined, undefined, undefined, startDate, endDate);

            return $http.get(url)
                .then(cacheQueriesPerUser, onFail);
        };

        /**
         * @todo: investigate sorting.
         */
        this.getTopicsPerState = function (startDate, endDate) {
            var roleSegment = mdlSvc.getRoleSegment(this.role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base + roleSegment + URLS.TOPICS_PER_STATE, undefined, undefined, undefined, undefined, undefined,  startDate, endDate);

            return $http.get(url)
                .then(cacheTopicsPerState, onFail);
        };
    }]);





