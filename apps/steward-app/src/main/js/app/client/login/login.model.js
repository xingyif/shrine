(function () {
    'use strict';

    angular
        .module('shrine.steward.login')
        .factory('loginModel', LoginModel);

    LoginModel.$inject = ['$http', '$q', 'constants'];
    function LoginModel($http, $q, constants) {

        // -- private --//
        var loginModel = this;
        var authFail = 'AuthenticationFailed';
        var url = constants.baseUrl + 'user/whoami/';

        return {
            login: authenticate
        };

        function authenticate() {
            return $http.get(url)
                .then(parse, reject);
        }

        function parse(result) {

            var data = result.data;

            return (data === 'AuthenticationFailed') ?
                reject(result) : {
                    success: true,
                    msg: result.data.statusText,
                    userId: result.data.userId,
                    roles: result.data.roles
                };
        }

        function reject(result, msg) {

            var statusText = result.data.statusText || result.data;

            var response = {
                success: false,
                msg: 'invalid login ' + statusText
            };

            return $q.reject(response);
        }
    }
})();
