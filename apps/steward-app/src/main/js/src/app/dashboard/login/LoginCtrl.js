angular.module("login", ['hms-authentication'])
    .controller('LoginCtrl', ['$scope', '$location', 'HMSAuthenticationService', function ($scope, $location, HMSAuthenticationService) {

        HMSAuthenticationService.ClearCredentials();
        $scope.loginFail = false;
        $scope.login = function () {
            HMSAuthenticationService.SetAuthHeader($scope.username.toLowerCase(), $scope.password);

            function onLoginSuccess (response) {
                HMSAuthenticationService.SetCredentials($scope.username.toLowerCase(), $scope.password, response.roles);
                $location.path('/topics/approved');
            }

            function onLoginFail(response) {
                HMSAuthenticationService.ClearCredentials();
                $scope.loginFail = true;
                $scope.username = $scope.password = '';
            }

            HMSAuthenticationService.Login()
                .then(onLoginSuccess, onLoginFail);
        };
    }]);


