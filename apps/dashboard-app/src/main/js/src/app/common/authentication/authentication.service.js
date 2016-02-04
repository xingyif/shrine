(function () {
    'use strict';

    // -- angular module -- //
    angular.module('shrine.commmon.authentication')
        .factory('AuthenticationService', AuthenticationService)


    AuthenticationService.$inject = ['$http', '$q', '$app']
    function AuthenticationService ($http, $q, $app) {


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


        // -- private methods -- //
        /**
        * Wrapper for authenticate call.
        */
        function login () {
            return authenticate();
        };


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
            if($app.globals.currentUser !== undefined) {
                $app.globals.currentUser.isLoggedIn = false;
            }

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
            if(result.data === Config.FailureResponse) {
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
            var response = {
                success: false,
                msg: "invalid login " + result.data.statusText
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
