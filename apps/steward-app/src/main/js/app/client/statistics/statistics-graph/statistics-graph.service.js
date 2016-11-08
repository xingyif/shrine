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
            getCountAsPercentage: getCountAsPercentage
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
            return 100 * (userQueryCount / maxQueryCount);
        }
    }

    Service = StatisticsGraphService;
})();

