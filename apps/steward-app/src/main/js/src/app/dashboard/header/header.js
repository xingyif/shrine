'use strict';

/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('stewardApp')
	.directive('header',function(){
		return {
        templateUrl:'src/app/dashboard/header/header.tpl.html',
        restrict: 'E',
        replace: true
    	}
	});


