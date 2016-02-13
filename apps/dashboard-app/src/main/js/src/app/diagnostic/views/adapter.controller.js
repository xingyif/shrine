(function () {
    'use strict';

    // -- register conroller with angular -- //
    angular.module('shrine-tools')
        .controller('AdapterController', AdapterController);

    /**
     *
     * @type {string[]}
     */
    AdapterController.$inject = ['$app'];
    function AdapterController ($app) {
        var vm = this;

        init();

        function init () {
            var all     = $app.model.cache['all'];
            var config  = $app.model.cache['config'];

            setAdapter(all);
            setConfiguration(config);
            setMappings(config);
        }



        function setAdapter (all) {
            vm.adapter  = {
                term:           "UKNOWN", //@todo:
                description:    all.adapter.result.response,
                success:        all.adapter.result.response.errorResponse === undefined
            };


        }

        function setConfiguration (config) {
            vm.configuration = {
                crcEndpointURL:     config.adapter.crcEndpointUrl,
                crcProjectId:       config.hiveCredentials.crcProjectId,
                domain:             config.hiveCredentials.domain,
                username:           config.hiveCredentials.username,
                password:           config.hiveCredentials.password,
                lockoutThreshold:   config.adapter.adapterLockoutAttemptsThreshold
            }
        }

        function setMappings (config) {
            vm.mappings = {
                mappsingsFilename:  config.adapter.adapterMappingsFilename
            };
        }

    }
})();
