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
                    console.log(o);
                    return o._2;
                }]).reverse();
            }

            return sortedUsers;
        }

        function getMaxUser(users) {
            var sortedUsers = getSortedUsers(users);
            return (!!sortedUsers.length) ? sortedUsers[0] : sortedUsers;
        }

        function getMaxUserQueryCount(users) {
            var maxUser = getMaxUser(users);
            return maxUser._2;
        }

        function getCountAsPercentage(userQueryCount, maxQueryCount) {
            var basePct = 20;
            return 100 * (userQueryCount / maxQueryCount) + basePct;
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

