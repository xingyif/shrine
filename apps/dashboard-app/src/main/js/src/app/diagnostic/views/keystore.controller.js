(function () {
    'use strict'

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('KeystoreController', KeystoreController);


    /**
     *
     * @type {string[]}
     */
    KeystoreController.$inject = ['$app']
    function KeystoreController ($app) {
        var vm = this;

        init();


        /**
         *
         */
        function init() {
            var all     = $app.model.cache['all'];
            var config  = $app.model.cache['config'];
            setKeystore(config);
            setCertificate(all, config)
        }


        /**
         *
         * @param all
         */
        function setKeystore (all) {
            vm.keystore = {
                file:       all.shrine.keystore.file,
                password:   "REDACTED"
            }
        }


        /**
         *
         * @param all
         * @param config
         */
        function setCertificate (all, config) {
            vm.certificate = {
                alias:          config.shrine.keystore.privateKeyAlias,
                owner:          "UNKNOWN", //todo config.keystore.certId.name,
                issuer:         "UNKNOWN", //@todo: verify the source,
                privateKeyAlias: config.shrine.keystore.privateKeyAlias //todo: Why are these the same?

            }
        }
    }
})();