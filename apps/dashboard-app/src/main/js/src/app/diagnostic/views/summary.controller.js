(function () {
    'use strict';

    // -- register controller with shrine-tools module
    angular.module('shrine-tools')
        .controller('SummaryController', SummaryController);


    /**
     * Summary Controller.
     *
     */
    SummaryController.$inject = ['$app', '$sce', '$log', '$timeout'];
    function SummaryController ($app, $sce, $log, $timeout) {
        var vm                  = this;
        var unknown             = 'UNKNOWN';
        vm.summary              = false;
        vm.networkHealth        = false;
        vm.summaryError         = false;
        vm.networkHealthError   = false;
        vm.i2b2Error            = false;
        vm.loading              = true;
        $app.model.reloadSummary = init;
        init();

        /**
         *
         */
        function init() {
            vm.loading = true;
            $app.model.getSummary()
                .then(setSummary, handleSummaryFailure);
            $app.model.getNetworkHealth()
                .then(setNetworkHealth, handleNetworkHealthFailure);

            $app.model.getI2B2()
                .then(setI2B2, handleI2B2Failure);
        }

        function handleSummaryFailure(failure) {
            vm.summaryError = failure;
            vm.loading = false
        }

        function handleNetworkHealthFailure(failure) {
            vm.networkHealthError = failure;
            vm.loading = false
        }

        function handleI2B2Failure(failure) {
            vm.i2b2Error = failure;
            vm.loading = false
        }

        function formatDate(maybeEpoch) {
            if (maybeEpoch && typeof maybeEpoch == "number") {
                var d = new Date(maybeEpoch);
                return $app.model.formatDate(d);
            } else {
                return unknown;
            }
        }


        /**
         *
         * @param summary
         */
        function setSummary(summary) {
            vm.loading = false;
            vm.summary = summary;
            if (vm.summary.adapterMappingsFileName === undefined) {
                vm.summary.adapterMappingsFileName = unknown;
            } else if (vm.summary.adapterMappingsDate === undefined) {
                vm.adapterMappingsDate = unknown;
            } else {
                vm.adapterMappingsDate = formatDate(vm.summary.adapterMappingsDate);
            }
            return this;
        }

        /**
         *
         * @param network health
         */
        function setNetworkHealth(networkHealth) {
            vm.loading = false;
            vm.networkHealth = networkHealth;

            return this;
        }

        function setI2B2(i2b2) {
            vm.ontProject = i2b2.ontProject;
        }
    }
})();
