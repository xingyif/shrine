(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('I2B2ConnectionsController', I2B2ConnectionsController);

    /**
     *
     */
    //todo: delete LOG
    I2B2ConnectionsController.$inject = ['$app', '$log'];
    function I2B2ConnectionsController($app, $log) {
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
            $log.warn(JSON.stringify(config));
            vm.connections  = {
                pmEndpointUrl:  config['shrine.pmEndpoint.url'],
                crcEndpointUrl: config['shrine.adapter.crcEndpointUrl'],
                ontEndpointUrl: config['shrine.ontEndpoint.url'],
                i2b2Domain:     config['shrine.hiveCredentials.domain'],
                username:       config['shrine.hiveCredentials.username'],
                crcProject:     config['shrine.hiveCredentials.crcProjectId'],
                ontProject:     config['shrine.hiveCredentials.ontProjectId']
            }

        }
    }
})();
