angular.module("history-model", ['model-service'])
    .service("HistoryMdl", ['$http', 'ModelService', '$app', function ($http, mdlSvc, $app) {
        var queryRecords    = [],
            totalCount      = 0,
            skipped         = 0,
            state           = "",
            URLS        = {
                REQUEST_HISTORY: "/queryHistory"
            };

        function HistoryMdl(role) {
            this.role = role;
        }

        function onFail(result) {
            alert("Get History Failed: " + result);
        }

        function parseHistory(result) {

            queryRecords = result.data.queryRecords;
            skipped      = result.data.skipped;
            totalCount   = result.data.totalCount;
            return {
                queryRecords:   queryRecords,
                numberSkipped:  skipped,
                totalCount:     totalCount
            };
        }

        /*
         A researcher can also see his own query history
         > curl -w " %{http_code}\n" -u ben:kapow -X GET "http://localhost:8080/steward/researcher/queryHistory"
         */

        HistoryMdl.prototype.getHistory = function (skip, limit, sortBy, sortDirection, topicId) {

            var request  = URLS.REQUEST_HISTORY;
            if (topicId !== undefined) {
                request += '/topic/' + topicId;
            }

            var roleSegment = mdlSvc.getRoleSegment(this.role, $app.globals.UserRoles),
                url         = mdlSvc.getURL(mdlSvc.url.base + roleSegment + request, skip, limit, undefined, sortBy, sortDirection);

            return $http.get(url)
                .then(parseHistory, onFail);
        };


        this.getInstance = function (role) {
            return new HistoryMdl(role);
        };
    }]);
