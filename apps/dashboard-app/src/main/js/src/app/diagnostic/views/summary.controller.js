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

            // $app.model.getConfig()
            //     .then(setConfig, onConfigFail);
        }


        /**
         *
         * @param summary
         */
        function setSummary(summary) {
            $log.warn(JSON.stringify(summary));
            //TODO FINISH
            // -- cache summary and all -- //
            // $app.model.cache['all']     = summary.all;
            // $app.model.cache['summary'] = summary;

            // -- set viewmodel  -- //
            vm.summary              = summary;
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
