(function () {
    'use strict';

    angular
        .module('shrine.common')
        .factory('CommonService', CommonService);

    /**
     * 
     */
    CommonService.$inject = ['DateService'];
    function CommonService(DateService) {

        return {
            dateService: DateService,
            hasAccess: hasAccess,
            toBase64: toBase64
        };

        /**
         * Verify that the intersection of user roles and the roles array.
         * @param user - user object containing array of roles.
         * @param rolesArray - an array of acceptable roles.
         */
        function hasAccess(user, rolesArray) {
            return !!(_.intersection(user.roles, rolesArray).length !== 0);
        }

        /**
         * Convert a string to BASE 64.
         * @param str
         */
        function toBase64(str) {
            return window.btoa(str);
        }
    }
})();
