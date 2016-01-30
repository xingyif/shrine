(function () {
    'use strict';

    angular
        .module('shrine-tools')
        .controller('STCtrl', DiagnosticController);

        DiagnosticController.$inject = ['$scope', '$location', '$app'];
        function DiagnosticController($scope, $location, $app) {
            $scope.$app     = $app;

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

            $scope.logout = function () {
                $location.path('/login');
            };

            $app.model.getSummary()
                .then(setSummary);

            function setSummary(data) {
                $scope.summary = data;
            }

        }

        DiagnosticLinker.$inject = ['scope'];
        function DiagnosticLinker(s) {

        }
})();





