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
        var vm          = this;
        var unknown     = 'UNKNOWN';
        vm.summaryError = false;
        vm.i2b2Error    = false;
        vm.loading      = true;
        $app.model.reloadSummary = init;
        init();

        /**
         *
         */
        function init() {
            vm.loading = true;
            var fifteenSeconds = 15*1000;
            $app.model.getSummary()
                .then(setSummary, handleSummaryFailure);

            $app.model.getI2B2()
                .then(setI2B2, handleI2B2Failure);

            $timeout(setTimeoutError, fifteenSeconds);
        }

        function setTimeoutError() {
            vm.loading = false;
        }

        function handleSummaryFailure(failure) {
            vm.summaryError = failure;
        }

        function handleI2B2Failure(failure) {
            vm.i2b2Error = failure;
        }

        function formatDate(maybeEpoch) {
            if (!(maybeEpoch && isFinite(maybeEpoch))) {
                return unknown;
            } else {
                var d = new Date(maybeEpoch);
                return $app.model.formatDate(d);
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
                vm.summary.adapterMappingsDate = unknown;
            } else {
                vm.summary.adapterMappingsDate = formatDate(vm.summary.adapterMappingsDate);
            }
            return this;
        }

        function setI2B2(i2b2) {
            vm.ontProject = i2b2.ontProject;
        }
    }
})();
