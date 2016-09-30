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

        init();

        function init () {
            $app.model.getAdapter()
                .then(setAdapter, handleFailure);
        }

        function handleFailure(failure) {
            // TODO: HANDLE FAILURE
            $log.error(JSON.stringify(failure));
        }


        //TODO: figure out what this wants to accomplish
        function setAdapter (adapter) {
            vm.adapter  = {
                term:           "TODO: NETWORK STATUS QUERY", //config.networkStatusQuery,
                success:        "TODO: ADAPTER RESULT SUCCESS" //config.adapter.result.response.errorResponse === undefined
            };

            // if (all.adapter.result.response.errorResponse) {
            //     vm.adapter.errorData = all.adapter.result.response.errorResponse.problem;
            // }
            // else {
            //     vm.adapter.description = all.adapter.result.response.runQueryResponse.queryResults.
            //         queryResult.setSize;
            //     vm.adapter.description += ' ' + all.adapter.result.response.runQueryResponse.queryResults.
            //             queryResult.resultType.description;
            // }
            setConfiguration(adapter);
        }

        function setConfiguration (adapter) {
            vm.configuration = {
                crcEndpointURL:     adapter.crcEndpointUrl,
                crcProjectId:       "TODO: CRC ID", //config.hiveCredentials.crcProjectId,
                domain:             "TODO: HIVE CREDENTIALS DOMAIN", //config.hiveCredentials.domain,
                username:           "TODO: HIVE CREDENTIALS USERNAME", //config.hiveCredentials.username,
                password:           "REDACTED", //config.hiveCredentials.password,
                lockoutThreshold:   adapter.adapterLockoutAttemptsThreshold
            };
            setMappings(adapter);
        }

        function setMappings (adapter) {
            vm.mappings = {
                mappingsFilename:  adapter.adapterMappingsFilename
            };
        }

    }
})();
