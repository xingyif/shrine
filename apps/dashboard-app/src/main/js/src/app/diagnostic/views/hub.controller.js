(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('HubController', HubController);


    /**
     *
     * @type {string[]}
     */
    HubController.$inject = ['$app', '$log'];
    function HubController ($app, $log) {
        var vm = this;
        vm.hubError = false;
        init();

        /**
         *
         */
        function init () {
            $app.model.getHub()
                .then(setDownstreamNodes, handleFailure);
        }

        function handleFailure (failure) {
            vm.hubError = failure;
        }

        /**
         *
         * @param hub
         */
        function setDownstreamNodes (hub) {
            vm.shouldQuerySelf = hub.shouldQuerySelf;

            vm.downstreamNodes = hub.downstreamNodes;

            if(vm.shouldQuerySelf === true) {
                vm.downstreamNodes.unshift({
                    name: 'self',
                    url:  'not applicable'
                });
            }
        }
    }
})();
