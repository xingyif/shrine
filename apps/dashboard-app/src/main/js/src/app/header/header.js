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
    function Header() {
        return {
            restrict:     'E',
            replace:      true,
            templateUrl:  'src/app/header/header.tpl.html',
            controller:   HeaderController,
            controllerAs: 'vm',
            link: HeaderLinker,
            scope: {
                title: '@'
            }
        }
    }

    /**
     *
     * @type {string[]}
     */
    HeaderController.$inject = ['$app', '$scope', '$location', '$window'];
    function HeaderController($app, $scope, $location, $window) {
        $scope.m = $app.model.m;
        $scope.goHome = goHome;

        function goHome() {

            $app.model.toDashboard.url = '';
            $app.model.m.siteAlias = '';
            clearCache();
            $location.url("/diagnostic/summary");
            $window.reload();
        }

        function clearCache() {
            for (var member in $app.model.cache) {
                if ($app.model.cache.hasOwnProperty(member)) {
                    delete $app.model.cache[member];
                }
            }
        }
    }

    /**
     *
     * @type {string[]}
     */
    HeaderLinker.$inject = ['scope', 'element', 'attributes'];
    function HeaderLinker(s, e, a) {
        var vm = s.vm;
    }

})();



