(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('LoginController', LoginController);

    LoginController.$inject = ['$location', 'AuthenticationService'];
    function LoginController ($location, authService) {

        // -- public -- //
        var vm          = this;
        vm.loginFail    = false;
        vm.login        = login;


        // -- clear the current credentials -- //
        authService.clearCredentials();


        // -- private -- //
        /**
         * User login.
         */
        function login () {

            // -- must set authorization header before -- //
            authService.setAuthorizationHeader(vm.username, vm.password);

            // -- service login -- //
            authService.login()
                .then(onLoginSuccess, onLoginFail);
        }


        /**
         * Process successful login.
         * @param response
         */
        function onLoginSuccess (response) {

            // -- save uers credentials -- //
            authService.setCredentials(vm.username, vm.password, response.roles);

            // -- go to default location -- //
            $location.path('/dashboard/diagnostics');
        }


        /**
         * Process failed login.
         * @param response
         */
        function onLoginFail(response) {

            // -- clear user credentials -- //
            authService.clearCredentials();
            vm.loginFail    = true;
            vm.username     = vm.password = '';
        }
    }
})();
