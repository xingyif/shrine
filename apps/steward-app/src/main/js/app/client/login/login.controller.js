(function () {
    'use strict';

    angular
        .module('shrine.steward.login');

    LoginController.$inject = ['LoginModel', 'LoginService', '$location', 'constants'];
    function LoginController(loginModel, loginService, $location, constants) {

        // -- public --//
        var login = this;
        login.loginFail = false;
        this.checkLogin = checkLogin;
        this.navigateToHome = navigateToHome;

        // -- private -- //
        function checkLogin() {
            loginService.setAuthHeader(login.username, login.password);
            loginService.login()
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
        }
    }
})();
