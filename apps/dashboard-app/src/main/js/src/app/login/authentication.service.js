(function () {
    'use strict';

    // -- angular module -- //
    angular.module('shrine-tools')
        .factory('AuthenticationService', AuthenticationService);


    AuthenticationService.$inject = ['$http', '$q', '$app', '$rootScope', '$interval', '$location'];
    function AuthenticationService ($http, $q, $app, $rootScope, $interval, $location) {

        idleHandle();

        // -- private const -- //
        var Config = {
            AuthenticationEndpoint: 'user/whoami',
            FailureResponse:        'AuthenticationFailed'
        };


        // -- public -- //
        return {
            login:                  login,
            setAuthorizationHeader: setAuthorizationHeader,
            setCredentials:         setCredentials,
            clearCredentials:       clearCredentials
        };


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
                $app.model.safeLogout();
            }

            $rootScope.idleBroadcast = function() {
                $rootScope.$broadcast(idleEvent);
            }
        }


        // -- private methods -- //
        /**
        * Wrapper for authenticate call.
        */
        function login () {
            return authenticate();
        }


        /**
         * Encrypt Authorization Data.
         * @param username
         * @param password
         * @returns {string}
         * @constructor
         */
        function getAuthorizationData(username, password) {
            return 'Basic ' + $app.utils.toBase64(username + ':' + password);
        }


        /**
         * Set the Authorization header for requests.
         * @param username
         * @param password
         * @constructor
         */
        function setAuthorizationHeader (username, password) {
            $http.defaults.headers.common['Authorization'] =
                getAuthorizationData(username, password);
        }


        /**
         * Set credentials of the current user.
         * @param username
         * @param password
         * @param roles
         * @constructor
         */
        function setCredentials(username, password, roles) {
            var data = getAuthorizationData(username, password)
            $app.utils.setAppUser(username, data, roles);
        };


        /**
         * Remove the credentials of the current user.
         */
        function clearCredentials () {
            $app.utils.deleteAppUser();
            $http.defaults.headers.common.Authorization = ' Basic ';
        }

        /**
         * Parse out authentication data.
         * @param result
         * @returns {*}
         */
        function parseResult(result) {
            //reject promise on fail.
            if(!result.data || result.data === Config.FailureResponse) {
                clearCredentials();
                return $q.reject(response);
            }

            var response = {
                success: true,
                msg:     result.data.statusText,
                userId:  result.data.userId,
                roles:   result.data.roles
            };

            return response;
        }


        /**
         * Reject invalid login.
         * @param result
         * @returns {Promise}
         */
        function rejectResult (result) {
            clearCredentials();
            var msg = result && result.data? "invalid login " + result.data.statusText : "invalid login";

            var response = {
                success: false,
                msg: msg
            };
            return $q.reject(response);
        }

        /**
         * Authentication Promise.
         * @returns {*}
         */
        function authenticate (baseUrl) {

            var url = $app.utils.getUrl(Config.AuthenticationEndpoint);

            return $http.get(url)
                .then(parseResult, rejectResult);
        }
    }
})();
