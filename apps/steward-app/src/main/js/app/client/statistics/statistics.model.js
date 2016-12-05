(function () {
    'use strict';

    angular
        .module('shrine.steward.statistics')
        .factory('StatisticsModel', StatisticsModel);

    StatisticsModel.$inject = ['$http', 'StewardService', 'HistoryModel'];
    function StatisticsModel($http, StewardService, HistoryModel) {
        var service = StewardService;
        var urls = {
            queriesPerUser: 'steward/statistics/queriesPerUser',
            topicsPerState: 'steward/statistics/topicsPerState',
            userQueryHistory: 'steward/queryHistory/user'
        };

        // -- public -- //
        return {
            getQueriesPerUser: getQueriesPerUser,
            getTopicsPerState: getTopicsPerState,
            getUserQueryHistory: getUserQueryHistory
        };

        // -- private -- //
        function getQueriesPerUser(startDate, endDate) {

            // -- make sure undefined is passed in -- //
            var skip, limit, state, sortBy, sortDirection;
            var url = service.getUrl(urls.queriesPerUser, skip, limit, state,
                sortBy, sortDirection, startDate, endDate);

            return $http.get(url)
                .then(parseQueriesPerUser, onFail);
        }

        function getTopicsPerState(startDate, endDate) {

            // -- make sure undefined is passed in -- //
            var skip, limit, state, sortBy, sortDirection;
            var url = service.getUrl(urls.topicsPerState, skip, limit, state,
                sortBy, sortDirection, startDate, endDate);

            return $http.get(url)
                .then(parseTopicsPerState, onFail);
        }

        function getUserQueryHistory(username, startDate, endDate) {
            var queryString = '&asJson=true';
            var skip, limit, state, sortBy, sortDirection;
            var url = service.getUrl(urls.userQueryHistory + '/' + username, skip,
                limit, state, sortBy, sortDirection, startDate, endDate) + queryString;
            return $http.get(url)
                .then(parseQueryHistory, onFail);
        }

        // -- private -- //
        function onFail(result) {
            alert('HTTP Request Fail: ' + result);
        }

        function parseQueriesPerUser(result) {

            var total = result.data.total,
                users = result.data.queriesPerUser;

            return {
                total: total,
                users: users
            };
        }

        function parseTopicsPerState(result) {

            var total = result.data.total,
                states = result.data.topicsPerState;

            return {
                total: total,
                states: states
            };
        }

        function parseQueryHistory(result) {
            return result.data;
        }
    }

})();
