(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('HubController', HubController);


    /**
     *
     * @type {string[]}
     */
    HubController.$inject = ['$app'];
    function HubController ($app) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            var config = $app.model.cache['config'];
            setDownstreamNodes(config);
        }


        /**
         *
         * @param config
         */
        function setDownstreamNodes (conf) {
            var config = angular.copy(conf);

            vm.shouldQuerySelf = config.shrine.hub.shouldQuerySelf;

            var nodes = config.shrine.hub.downstreamNodes;
            vm.downstreamNodes = [];

            for (var key in nodes) {
                if (nodes.hasOwnProperty(key)) {
                    vm.downstreamNodes.push({name: key, url: nodes[key]})
                }
            }

            if(vm.shouldQuerySelf === true) {
                vm.downstreamNodes.unshift({
                    name: 'self',
                    url:  'not applicable'
                });
            }
        }
    }
})();
