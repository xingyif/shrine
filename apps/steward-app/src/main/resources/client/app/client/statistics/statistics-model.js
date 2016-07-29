(function() {
    'use strict';

    StatisticsModel.$inject = ['$http','StewardService'];
    function StatisticsModel($http, StewardService) {
        var service = StewardService;
        var urls = {
            queriesPerUser: 'steward/statistics/queriesPerUser',
            topicsPerState: 'steward/statistics/topicsPerState'
        };

        // -- public -- //
        return {
            getQueriesPerUser: getQueriesPerUser,
            getTopicsPerState: getTopicsPerState
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

            var total  = result.data.total,
                states = result.data.topicsPerState;

            return {
                total:  total,
                states: states
            };
        }
    }

})();
