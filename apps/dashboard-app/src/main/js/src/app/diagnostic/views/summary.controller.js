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
            $app.model.getHappyAll()
                .then(setSummary);

            $app.model.getConfig()
                .then(setConfig);
        }


        /**
         *
         * @param summary
         */
        function setSummary(happyAll) {

            // -- cache summary and all -- //
            $app.model.cache['all']     = happyAll.all;
            $app.model.cache['summary'] = happyAll.summary;

            // -- set viewmodel  -- //
            vm.summary              = happyAll.summary;
            return this;
        }


        /**
         *
         * @returns {*}
         */
        function getConfig() {
             return $app.model.getConfig();
            return this;
        }


        /**
         *
         * @param config
         */
        function setConfig(config) {

            // -- cache the config --
            $app.model.cache['config'] =  config;
            vm.config = config;
            return this;
        }
    }
})();
