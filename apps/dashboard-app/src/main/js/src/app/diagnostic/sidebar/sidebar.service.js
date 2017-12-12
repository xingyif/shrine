

/**
 * Created by ben on 1/21/16.
 */
(function () {
    'use strict';

    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('SidebarService', SidebarService);


    // -- factory definition -- //
    SidebarService.$inject = ['SidebarModel']
    function SidebarService (m) {
        return {
            getSummary: getSummary,
            getNetworkHealth: getNetworkHealth
        }

        /**
         *
         * @returns {*}
         */
        function getSummary() {
           return m.getSummary()
                .then(function (data) {
                    return data
                });
        }

        /**
        *
        * @returns {*}
        */
        function getNetworkHealth() {
            return m.getNetworkHealth()
                .then(function (data) {
                    return data
                });
        }
    }
})();
