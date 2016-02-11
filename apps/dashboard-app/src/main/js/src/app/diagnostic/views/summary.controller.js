(function () {
    'use strict';

    // -- register controller with shrine-tools module
    angular.module('shrine-tools')
        .controller('SummaryController', SummaryController);

    /**
     * Summary Controller.
     *
     */
    SummaryController.$inject = ['$app']
    function SummaryController ($app) {
        var vm = this;

        init();

        
        /**
         *
         */
        function init() {
            $app.model.getSummary()
                .then(setSummary)
                .then(getConfig)
                .then(setConfig);
        }


        /**
         *
         * @param summary
         */
        function setSummary(summary) {
            vm.summary = summary;
        }


        /**
         *
         * @returns {*}
         */
        function getConfig() {
             return $app.model.getConfig();
        }


        /**
         *
         * @param config
         */
        function setConfig(config) {

            // -- cache the config --
            $app.model.cache['config'] =  config;
            vm.config = config;
        }
    }
})();
