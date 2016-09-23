(function () {
    'use strict';

    angular
        .module('shrine.steward')
        .run(StewardRunner);

    /**
    * App Run Phase - Set up listener to verify user has access.
    */
    StewardRunner.$inject = ['$rootScope', '$location', 'StewardService'];
    function StewardRunner($rootScope, $location, StewardService) {

        var defaultRoute = StewardService.constants.defaultRoute;
        var path = $location.path();

        $rootScope.$on('$locationChangeStart', verifyIdentity);

        function verifyIdentity(event, next, current) {
            if (isUserNotLoggedIn()) {
                $location.path(defaultRoute);
            }
        }

        function isUserNotLoggedIn() {
            var currentUser = StewardService.getAppUser();
            return (!currentUser || !currentUser.isLoggedIn);
        }
    }
})();
