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
        vm.i2b2Error = false;
        init();


        /**
         *
         */
        function init () {
            $app.model.getI2B2()
                .then(setConnections, handleFailure);
        }

        function handleFailure(failure) {
            //TODO: HANDLE FAILURE BETTER
            vm.i2b2Error = failure;
        }


        /**
         *
         */
        function setConnections (i2b2) {
            // @todo: make sure config existes in cache if so cull from cached config, if not make rest call to endpoint,
            vm.connections  = {
                pmEndpointUrl:  i2b2.pmUrl,
                crcEndpointUrl: i2b2.crcUrl,
                ontEndpointUrl: i2b2.ontUrl,
                i2b2Domain:     i2b2.i2b2Domain,
                username:       i2b2.username,
                crcProject:     i2b2.crcProject,
                ontProject:     i2b2.ontProject
            }

        }
    }
})();
