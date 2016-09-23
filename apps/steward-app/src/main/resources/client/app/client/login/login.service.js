(function () {
    'use strict';

    angular
        .module('shrine.steward.login')
        .factory('LoginService', LoginService);

    LoginService.$inject = ['$http','StewardService', '$rootScope', '$location', '$interval'];
    function LoginService($http, StewardService, $rootScope, $location, $interval) {
        var svc = StewardService;

        idleHandle();

        return {
            setAuthHeader: setAuthHeader,
            setCredentials: setCredentials,
            clearCredentials: clearCredentials
        };

        function setAuthHeader(username, password) {
            var authdata = svc.commonService.toBase64(username + ':' + password);
            $http.defaults.headers.common['Authorization'] =  'Basic ' + authdata;
        }

        function setCredentials(username, password, roles) {
            var authdata = svc.commonService.toBase64(username + ':' + password);
            svc.setAppUser(username, authdata, roles);
        }

        function clearCredentials() {
            var user = svc.getAppUser();
            svc.deleteAppUser();
            $http.defaults.headers.common.Authorization = ' Basic ';
        }

        function idleHandle() {
            // -- auto logout on idle -- //
            var twentyMinutes = 20*60*1000;
            var logoutPromise = $interval(timeout, twentyMinutes);
            var idleEvent = 'idleEvent';
            $rootScope.$on('$destroy', function () {
                $interval.cancel(logoutPromise);
            });
            $rootScope.$on(idleEvent, function () {
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
            }
        }

    }
})();
