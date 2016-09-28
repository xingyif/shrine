(function () {
    'use strict';

    // -- register controller with shrine-tools module
    angular.module('shrine-tools')
        .controller('SummaryController', SummaryController);


    /**
     * Summary Controller.
     *
     */
    SummaryController.$inject = ['$app', '$sce', '$log']
    function SummaryController ($app, $sce, $log) {
        var vm          = this;

        init();

        
        /**
         *
         */
        function init() {
            $app.model.getSummary()
                .then(setSummary, onHappyFail);

            $app.model.getConfig()
                .then(setConfig, onConfigFail);
        }


        /**
         *
         * @param summary
         */
        function setSummary(happyAll) {
            //TODO FINISH
            // -- cache summary and all -- //
            $app.model.cache['all']     = happyAll.all;
            $app.model.cache['summary'] = happyAll;

            // -- set viewmodel  -- //
            vm.summary              = happyAll;
            $log.warn(JSON.stringify(vm.summary));
            return this;
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
            vm.config                   = config;
            return this;
        }




        /**
         *
         * @param data
         */
        function onHappyFail(data) {
            vm.trustedHtml  = $sce.trustAsHtml(data);
        }


        /**
         *
         * @param data
         */
        function onConfigFail (data) {
            vm.trustedHtml  = $sce.trustAsHtml(data);
        }
    }
})();
