'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('shrine-tools')
	.directive('header', function() {
		return {
        templateUrl:'src/app/dashboard/header/header.tpl.html',
        restrict: 'E',
        replace: true
    	}
	});


