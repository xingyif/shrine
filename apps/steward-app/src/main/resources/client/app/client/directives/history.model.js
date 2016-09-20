(function () {

    //todo: create own module for shared directives?
    angular.module('shrine.steward')
        .factory('HistoryModel', HistoryModel);

    HistoryModel.$inject = ['$http', '$q', 'HistoryService'];
    function HistoryModel($http, $q, service) {
        var historyModel = this;

        var urls = {
            researcherHistory: 'researcher/queryHistory',
            stewardHistory: 'steward/queryHistory'
        };

        // -- public -- //
        return {
            getResearcherHistory: getResearcherHistory,
            getStewardHistory: getStewardHistory
        }

        // -- private -- //

        function getResearcherHistory(skip, limit, sortBy, sortDirection, topicId) {
            return getHistory(urls.researcherHistory, skip, limit, sortBy, sortDirection, topicId);
        }

        function getStewardHistory(skip, limit, sortBy, sortDirection, topicId) {
            return getHistory(urls.stewardHistory, skip, limit, sortBy, sortDirection, topicId);
        }

        function getHistory(request, skip, limit, sortBy, sortDirection, topicId, state) {

            if (topicId !== undefined) {
                request += '/topic/' + topicId;
            }

            var url = service.getUrl(request, skip, limit, state, sortBy, sortDirection);

            return $http.get(url)
                .then(parseHistory, onFail);
        }

        function onFail(result) {
            alert("Get History Failed: " + result);
        }

        function parseHistory(result) {
            var queryRecords = result.data.queryRecords;
            var skipped = result.data.skipped;
            var totalCount = result.data.totalCount;

            return {
                queryRecords: queryRecords,
                numberSkipped: skipped,
                totalCount: totalCount
            };
        }
    }
})();

