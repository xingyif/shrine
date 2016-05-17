(function () {
    'use strict';

    // -- angular -- //
    angular.module('shrine-tools')
        .directive('nodeMenu', NodeMenu);

    /**
     * Directive Config
     * @returns {{templateUrl: string, restrict: string, replace: boolean, link: NodeMenuLinker, controller: NodeMenuController, controllerAs: string}}
     * @constructor
     */
    function NodeMenu () {
        var nodeMenu =  {
            templateUrl:    'src/app/diagnostic/sidebar/node-menu.tpl.html',
            restrict:       'A',
            replace:        true,
            link:           NodeMenuLinker,
            controller:     NodeMenuController,
            controllerAs:   'nmVM',
            scope: {
                menuId:  '@'
            }
        };

        return nodeMenu;
    }

    /**
     * Directive Controller
     * @controller
     */
    NodeMenuController.$inject = ['$scope']
    function NodeMenuController (s) {
        var _vm = this;


    }

    /**
     * Directive Linker.
     * @linker
     */
    NodeMenuLinker = ['scope', 'element', 'attribuates', 'nmVM']
    function NodeMenuLinker (s, e, a, n) {

    }

})();
