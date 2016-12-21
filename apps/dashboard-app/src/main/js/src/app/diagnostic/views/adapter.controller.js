(function () {
    'use strict';

    // -- register conroller with angular -- //
    angular.module('shrine-tools')
        .controller('AdapterController', AdapterController);

    /**
     *
     * @type {string[]}
     */
    AdapterController.$inject = ['$app', '$log'];
    function AdapterController ($app, $log) {
        var vm = this;
        vm.adapterError = false;
        vm.i2b2Error = false;
        vm.summaryError = false;

        init();

        function init () {
            $app.model.getAdapter()
                .then(setAdapter, handleAdapterError)
                .then($app.model.getI2B2)
                .then(setI2B2, handlei2b2Error);

            $app.model.getSummary()
                .then(setSummary, handleSummaryError);
        }

        function handleAdapterError(failure) {
            vm.adapterError = failure;
        }

        function handlei2b2Error(failure) {
            vm.i2b2Error = failure;
        }

        function handleSummaryError(failure) {
            vm.summaryError = failure;
        }


        function setSummary (summary) {
            vm.adapter  = {
                term:           summary.ontologyTerm, //config.networkStatusQuery,
                success:        summary.queryResult.response.problemDigest === undefined
            };

            if (summary.queryResult.response.problemDigest !== undefined) {
                vm.adapter.errorData = summary.queryResult.response.problemDigest;
            }
            else {
                //TODO FIGURE OUT THE CORRECT FIELDS FOR SUCCESSFUL QUERY RESULT
                vm.adapter.description =  summary.queryResult.response.singleNodeResult.setSize;
                vm.adapter.description += ' ';
                vm.adapter.description += summary.queryResult.response.singleNodeResult.resultType
                                            .i2b2Options.description;
            }
        }


        function formatDate(maybeEpoch) {
            if (!(maybeEpoch && isFinite(maybeEpoch))) {
                return 'UNKNOWN';
            } else {
                var d = new Date(maybeEpoch);
                return $app.model.formatDate(d);
            }
        }

        function setAdapter (adapter) {
            vm.mappings = {
                mappingsFilename:  adapter.adapterMappingsFilename,
                mappingsDate:      formatDate(adapter.adapterMappingsDate)
            };

            vm.configuration = {
                crcEndpointURL:     adapter.crcEndpointUrl,
                crcProjectId:       "", //config.hiveCredentials.crcProjectId,
                domain:             "", //config.hiveCredentials.domain,
                username:           "", //config.hiveCredentials.username,
                password:           "REDACTED", //config.hiveCredentials.password,
                lockoutThreshold:   adapter.adapterLockoutAttemptsThreshold
            };
        }

        function setI2B2 (i2b2) {
            vm.configuration.crcProjectId = i2b2.crcProject;
            vm.configuration.domain = i2b2.i2b2Domain;
            vm.configuration.username = i2b2.username;
        }
    }
})();
