'use strict';
    angular.module('shrine-tools')
.controller('MainCtrl', ['$rootScope', '$scope', '$location', '$app', 'STMdl', function ($rootScope, $scope, $location, $app, STMdl) {

    $scope.bannerUrl = '';
    $scope.helpUrl   = '';

    $scope.$app = $app;

    $scope.getConfigData = function () {
        STMdl.getConfig()
            .then(function (data) {
                $scope.bannerUrl = data.data.banner;
                $scope.helpUrl   = data.data.help;
            });
    };

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
        $location.path('/dashboard/login');
    };

    $scope.getConfigData();
}]);

