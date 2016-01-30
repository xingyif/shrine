'use strict';
//https://shrine-qa1.catalyst:6443/shrine/rest/happy/keystore
/**
 * @ngdoc directive
 * @name izzyposWebApp.directive:adminPosHeader
 * @description
 * # adminPosHeader
 */
angular.module('shrine-tools')
	.directive('header', function() {
		return {
			templateUrl:'src/app/header/header.tpl.html',
			restrict: 'E',
			replace: true,
			//@todo: closing scope will break functionality username and logout
			scope: {
				title: '@'
			}
    	}
	});


