(function () {
    'use strict';

    angular
        .module('shrine.steward.login')
        .factory('LoginModel', LoginModel);

    LoginModel.$inject = ['$http', '$q', 'StewardService'];
    function LoginModel($http, $q, service) {

        // -- private --//
        var loginModel = this;
        var authFail = 'AuthenticationFailed';
        var url = service.getUrl() + 'user/whoami/';

        return {
            login: authenticate
        };

        function authenticate() {
            return $http.get(url)
                .then(parse, reject);
        }

        function parse(result) {

            return (result.data === 'AuthenticationFailed') ?
                reject(result) : {
                    success: true,
                    msg: result.data.statusText,
                    userId: result.data.userId,
                    roles: result.data.roles
                };
        }

        function reject(result, msg) {

            result.data = result.data || 'xhr request timed out';

            var response = {
                success: false,
                message: 'invalid login: ' + result.data
            };

            return $q.reject(response);
        }
    }
})();
