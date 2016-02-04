(function () {
    'use strict';

    /**
     * App Run Phase - Set up listener to verify user has access.
     */
    angular
        .module('shrine-tools')
        .run(function ($rootScope, $location, $app) {

            $rootScope.$on( "$locationChangeStart", function (event, next, current) {
                // redirect to login page if not logged in
                if ($location.path() !== '/login' && (!$app.globals.currentUser || !$app.globals.currentUser.isLoggedIn)) {
                    $location.path('/login');
                }
            });
        })
})();