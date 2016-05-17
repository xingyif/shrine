(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('I2B2ConnectionsController', I2B2ConnectionsController);

    /**
     *
     */
    I2B2ConnectionsController.$inject = ['$app'];
    function I2B2ConnectionsController($app) {
        var vm = this;

        init();


        /**
         *
         */
        function init () {
            setConnections();
        }


        /**
         *
         */
        function setConnections () {

            // @todo: make sure config exists in cache if so cull from cached config, if not make rest call to endpoint,
            var config      = $app.model.cache['config'];
            vm.connections  = {
                pmEndpointUrl:  config.pmEndpoint.url,
                crcEndpointUrl: config.adapter.crcEndpointUrl,
                ontEndpointUrl: config.ontEndpoint.url,
                i2b2Domain:     config.hiveCredentials.domain,
                username:       config.hiveCredentials.username,
                crcProject:     config.hiveCredentials.crcProjectId,
                ontProject:     config.hiveCredentials.ontProjectId
            }

        }
    }
})();
