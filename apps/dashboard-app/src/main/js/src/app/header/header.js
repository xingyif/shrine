(function () {
	'use strict';


	// -- register directive with angular -- //
	angular.module('shrine-tools')
		.directive('header', Header);


	/**
	 *
	 * @returns {{restrict: string, replace: boolean, templateUrl: string,
	 * controller: HeaderController, controllerAs: string, link: HeaderLinker, scope: {title: string}}}
	 * @constructor
	 */
	function Header () {
		return {
			restrict: 		'E',
			replace: 		true,
			templateUrl: 	'src/app/header/header.tpl.html',
			controller: 	HeaderController,
			controllerAs: 	'vm',
			link:			HeaderLinker,
			scope: {
				title: '@'
			}
		}
	}


	/**
	 *
	 * @type {string[]}
	 */
	HeaderController.$inject = ['$app', '$scope', '$log'];
	function HeaderController($app, $scope, $log) {
		$scope.m = $app.model.m;
	}

	/**
	 *
	 * @type {string[]}
	 */
	HeaderLinker.$inject = ['scope', 'element', 'attributes' ];
	function HeaderLinker (s, e, a) {
		var vm = s.vm;
	}

})();



