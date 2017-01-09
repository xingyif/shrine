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
            getMinUserQueryCount: getMinUserQueryCount,
            getMinUser: getMinUser,
            formatUsername: formatUsername,
            clearUsers: clearUsers
        };

        function getSortedUsers(users) {
            if (!sortedUsers.length) {
                sortedUsers = _.sortBy(users, [function (o) {
                    return o._2;
                }]);
            }

            return sortedUsers;
        }

        function getMaxUser(users) {
            var sortedUsers = getSortedUsers(users);
            var length = sortedUsers.length;
            return (length) ? sortedUsers[length - 1] : sortedUsers;
        }

        function getMinUser(users) {
            var sortedUsers = getSortedUsers(users);
            return (sortedUsers.length) ? sortedUsers[0] : sortedUsers;
        }

        function getMaxUserQueryCount(users) {
            var maxUser = getMaxUser(users);
            return maxUser._2;
        }

        function getMinUserQueryCount(users) {
            var minUser = getMinUser(users);
            return minUser._2;
        }

        function getCountAsPercentage(userQueryCount, minQueryCount, maxQueryCount) {
            var pct = 50 * (userQueryCount / maxQueryCount);
            return pct + 15;
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

