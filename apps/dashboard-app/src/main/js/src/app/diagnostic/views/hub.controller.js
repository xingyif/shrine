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
        function setDownstreamNodes (config) {

            vm.shouldQuerySelf = config.hub.shouldQuerySelf;
            vm.downstreamNodes = config.hub.downstreamNodes;

            if(vm.shouldQuerySelf === true) {
                vm.downstreamNodes.unshift({
                    name: 'self',
                    url:  $app.utils.getUrl('')
                });
            }
        }
    }
})();
