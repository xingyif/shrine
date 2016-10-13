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
        var unknown      = 'UNKNOWN';
        init();

        
        /**
         *
         */
        function init() {
            $app.model.getSummary()
                .then(setSummary, handleFailure);

            $app.model.getI2B2()
                .then(setI2B2, handleFailure);

            vm.formatDate = formatDate;
        }

        function handleFailure(failure) {
            //TODO: HANDLE FAILURE
            $log.error(JSON.stringify(failure));
        }

        function formatDate(maybeEpoch) {
            if (!(maybeEpoch && isFinite(maybeEpoch))) {
                return unknown;
            } else {
                var d = new Date(maybeEpoch);
                return d.toUTCString();
            }
        }


        /**
         *
         * @param summary
         */
        function setSummary(summary) {
            // -- set viewmodel  -- //
            vm.summary              = summary;
            if (vm.adapterMappingsFileName === undefined) {
                vm.adapterMappingsFileName = unknown;
            } else if (vm.adapterMappingsDate === undefined) {
                vm.adapterMappingsDate = unknown;
            }
            return this;
        }

        function setI2B2(i2b2) {
            vm.ontProject = i2b2.ontProject;
        }
    }
})();
