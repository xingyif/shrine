

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
            getAdapterQueryTest: getAdapterQueryTest
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
        function getAdapterQueryTest() {
            return m.getAdapterQueryTest()
                .then(function (data) {
                    return data
                });
        }
    }
})();
