(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .service('StatisticsGraphService', StatisticsGraphService);

    function StatisticsGraphService() {
        return {
            geSortedUser: getSortedUsers,
            getMaxUser: getMaxUser,
            getMaxUserQueries: getMaxUserQueries
        };

        function getSortedUsers(users) {
            _.sortBy(users, [function (o) { return o._2; }]);
        }

        function getMaxUser(users) {
            var sortedUsers = getSortedUsers(users);
            return (!!sortedUsers.length) ? sortedUsers[0] : sortedUsers;
        }

        function getMaxUserQueries(users) {
            var maxUser = getMaxUser(users);
            return maxUser._2;
        }
    }
})();
