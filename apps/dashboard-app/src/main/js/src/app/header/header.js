(function () {
	'use strict';


	// -- register directive with angular -- //
	angular.module('shrine-tools', ['ngRoute'])
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
	HeaderController.$inject = ['$app', '$scope', '$location', '$route'];
	function HeaderController($app, $scope, $location, $route) {
		$scope.m = $app.model.m;
		$scope.goHome = goHome;


        function goHome() {
            //todo remove duplication with dashboardcontroller
            $app.model.toDashboard.url = '';
            $app.model.m.siteAlias = '';
            clearCache();
            $location.url("/diagnostic/summary");
            $route.reload();
        }

        function clearCache() {
            for (var member in $app.model.cache) {
                if($app.model.cache.hasOwnProperty(member)) delete $app.model.cache[member];
            }
        }
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



