(function () {
    'use strict';

    // -- register with angular -- //
    angular.module('shrine-tools')
        .controller('QEPController', QEPController);


    /**
     *
     * @type {string[]}
     */
    QEPController.$inject = ['$app'];
    function QEPController ($app) {
        var vm = this;

        init();

        function init () {
            $app.model.getQep()
                .then(setQep, handleFailure);
            setIsDownstream(config);
            setStewardEnabled(config);
            setBroadcasterUrl(config);
            setSteward(config);
        }

        function handleFailure(failure) {
            //TODO: HANDLE FAILURE
            $app.error(JSON.stringify(failure));
        }

        function setQep(qep) {
            vm.isStewardEnabled = ;
            vm.steward = ;
            vm.isDownstream = ;
            vm.broadcasterUrl = ;
        }


        /**
         *
         * @param config
         */
        function setIsDownstream (config) {
            vm.isDownstream = config.isHub === false;
        }


        /**
         *
         * @param config
         */
        function setStewardEnabled (config) {
            vm.isStewardEnabled = config.queryEntryPoint.shrineSteward !== undefined;
        }


        /**
         *
         */
        function setBroadcasterUrl (config) {
            vm.broadcasterUrl = (vm.isDownstream === true && config.queryEntryPoint.broadcasterServiceEndpointUrl !== undefined)?
                config.queryEntryPoint.broadcasterServiceEndpointUrl : "UNKNOWN";
        }


        /**
         *
         */
        function setSteward (config) {
            if(vm.isStewardEnabled === true && vm.isStewardEnabled === true) {
                vm.steward = {
                    qepUsername:    config.queryEntryPoint.shrineSteward.qepUserName,
                    stewardBaseUrl: config.queryEntryPoint.shrineSteward.stewardBaseUrl,
                    password:       "REDACTED"
                }
            }

        }
    }
})();
