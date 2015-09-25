'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('stewardApp')
	.directive('headerNotification',function(){
		return {
        templateUrl:'src/app/header/header-notification/header-notification.tpl.html',
        restrict: 'E',
        replace: true,
    	}
	});


