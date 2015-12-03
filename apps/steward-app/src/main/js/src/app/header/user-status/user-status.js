'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('stewardApp')
	.directive('userStatus',function(){
		return {
        templateUrl:'src/app/header/user-status/user-status.tpl.html',
        restrict: 'E',
        replace: true,
    	}
	});


