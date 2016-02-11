(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('ShrineConfigurationController', ShrineConfigurationController);

    ShrineConfigurationController.$inject = ['$app'];
    function ShrineConfigurationController ($app) {
        var vm = this;

        init();

        /**
         *
         */
        function init () {
            $app.model.getConfig()
                .then(setConfig)
        }

        /**
         *
         * @param configuration
         */
        function setConfig (config) {
            vm.config = config;
        }
    }
})();
