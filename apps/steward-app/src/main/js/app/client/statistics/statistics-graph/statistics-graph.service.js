var Service;

(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .service('StatisticsGraphService', StatisticsGraphService);

    var sortedUsers = [];

    function StatisticsGraphService() {
        return {
            getSortedUsers: getSortedUsers,
            getMaxUser: getMaxUser,
            getMaxUserQueryCount: getMaxUserQueryCount,
            getCountAsPercentage: getCountAsPercentage,
            formatUsername: formatUsername,
            clearUsers: clearUsers
        };

        function getSortedUsers(users) {
            if (!sortedUsers.length) {
                sortedUsers = _.sortBy(users, [function (o) {
                    return o._2;
                }]);
                sortedUsers.reverse();
            }

            return sortedUsers;
        }

        function getMaxUser(users) {
            var sortedUsers = getSortedUsers(users);
            var length = sortedUsers.length;
            return (length) ? sortedUsers[0] : sortedUsers;
        }

        function getMaxUserQueryCount(users) {
            var maxUser = getMaxUser(users);
            return maxUser._2;
        }

        function getCountAsPercentage(userQueryCount, maxQueryCount) {
            var pct = 50 * (userQueryCount / maxQueryCount);
            return pct + 20;
        }

        function formatUsername(username) {
            return (username.length > 10) ? username.substring(0, 10) + '...' : username;
        }

        function clearUsers() {
            sortedUsers = [];
        }
    }

    Service = StatisticsGraphService;
})();

