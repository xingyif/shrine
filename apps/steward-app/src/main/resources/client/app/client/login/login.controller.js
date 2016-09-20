(function () {
    'use strict';

    angular
        .module('shrine.steward.login')
        .controller('LoginController', LoginController);

    LoginController.$inject = ['LoginModel', 'LoginService', '$location', 'constants'];
    function LoginController(loginModel, loginService, $location, constants) {

        // -- public --//
        var login = this;
        login.loginFail = false;
        this.checkLogin = checkLogin;

        // -- private -- //
        function checkLogin() {
            loginService.setAuthHeader(login.username, login.password);
            loginModel.login()
                .then(navigateToHome, setLoginToError);
        }

        function navigateToHome(response) {
            loginService.setCredentials(login.username, login.password, response.roles);
            $location.path(constants.homeRoute);
        }

        function setLoginToError(response) {
            loginService.clearCredentials();
            login.loginFail = true;
            login.username = login.password = '';
            login.status = response.message;
        }
    }
})();
