'use strict';
angular.module('shrine-tools')
    .directive('userStatus', function () {
        return {
            templateUrl: 'src/app/header/user-status/user-status.tpl.html',
            restrict: 'E',
            replace: true
        };
    });

