'use strict';
angular.module('stewardApp')
    .controller('MainCtrl', ['$rootScope', '$scope', '$location', '$app', 'AppMdl', '$log', function ($rootScope, $scope, $location, $app, AppMdl, $log) {

        idleHandle();

        $scope.getUsername = function () {
            return ($app.globals.currentUser) ?
                $app.globals.currentUser.username : '';
        };

        $scope.getRole = function () {
            if (!$app.globals.currentUser) {
                return '';
            }
            var idx = $app.globals.currentUser.roles.length - 1;
            return $app.globals.currentUser.roles[idx];
        };

        $scope.isUserLoggedIn = function () {
            return ($app.globals.currentUser !== undefined && $app.globals.currentUser.isLoggedIn === true);
        };

        function idleHandle() {
            // -- auto logout on idle -- //
            var twentyMinutes = 5000;
            var logoutPromise = $interval(timeout, twentyMinutes);
            var idleEvent = 'idleEvent';
            $rootScope.$on('$destroy', function () {
                $interval.cancel(logoutPromise);
            });
            $rootScope.$on(idleEvent, function () {
                $log.warn('heyo!');
                $interval.cancel(logoutPromise);
                logoutPromise = $interval(timeout, twentyMinutes);
            });


            /**
             * When the interval is called, that means the user has gone idle, so we
             * clear their credentials then navigate them back to the home page.
             */
            function timeout() {
                clearCredentials();
                $location.url("/login");
            }

            $rootScope.idleBroadcast = function() {
                $rootScope.$broadcast(idleEvent);
                $log.warn('hello');
            }
        }


    }]);

