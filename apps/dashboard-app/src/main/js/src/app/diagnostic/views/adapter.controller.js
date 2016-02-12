(function () {
    'use strict';

    // -- register conroller with angular -- //
    angular.module('shrine-tools')
        .controller('AdapterController', AdapterController);

    /**
     *
     * @type {string[]}
     */
    AdapterController.$inject = ['$app'];
    function AdapterController ($app) {
        var vm = this;

        //init();

        function init () {
            $app.model.getAdapter()
                .then(setAdapter)
                .then(setConfig);
        }


        /**
         *
         * @param config
         */
        function setConfig(config) {

            // -- get config from cache -- //
            vm.config =  $app.model.cache['config']
        }


        /**
         *
         * @param adapter
         */
        function setAdapter(adapter) {
            vm.adapter = adapter;
        }

    }
})();
