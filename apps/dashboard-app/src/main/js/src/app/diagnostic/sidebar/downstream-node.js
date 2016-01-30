(function () {
    'use strict';

    // -- angular -- //
    angular.module('shrine-tools')
        .directive('downstreamNode', DownstreamNode);


    /**
     *
     * @returns {{restict: string, replace: boolean, templateUrl: string, scope: {}, controller: DownstreamNodeController, link: DownstreamNodeLinker, controllerAs: string}}
     * @constructor
     */
    function DownstreamNode () {
        var downstreamNode  = {
            restict: 'E',
            replace: true,
            templateUrl: 'src/app/diagnostic/sidebar/downstream-node.tpl.html',
            scope: {
                nodeData: '=',
                nodeIndex: '@'
            },
            controller: DownstreamNodeController,
            link: DownstreamNodeLinker,
            controllerAs: 'dsnVM'
        };

        return downstreamNode;
    };


    /**
     *
     * @constructor
     */
    DownstreamNodeController.$inject = ['$scope'];
    function DownstreamNodeController (s) {
        var _vm = this;
        _vm.nodeIndex   = s.nodeIndex;
        _vm.nodeData    = s.nodeData;
        _vm.addMenu     = false;
    }


    /**
     *
     * @param s
     * @param e
     * @param a
     * @param d
     * @constructor
     */
    //DownstreamNodeController.$inject ['scope', 'element', 'attributes', 'dsnVM'];
    function DownstreamNodeLinker (s, e, a, d) {

    }
})();
