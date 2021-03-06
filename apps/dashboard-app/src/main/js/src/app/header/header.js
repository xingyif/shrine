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
    HeaderController.$inject = ['$app', '$scope', '$location'];
    function HeaderController($app, $scope, $location) {
        $scope.m = $app.model.m;
        $scope.goHome = goHome;

        function goHome() {

            $app.model.toDashboard.url = '';
            $app.model.m.siteAlias = '';
            clearCache();
            $location.url("/diagnostic/summary");
            if ($app.model.reloadSummary) {
                $app.model.reloadSummary();
                // This is kind of gross, but the only way to reload the
                // summary is to reinitialize it, and as they live in
                // separate controllers we have to communicate through the model.
            }
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



