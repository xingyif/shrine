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
            setKeystore(all);
            setCertificate(all, config)
        }


        /**
         *
         * @param all
         */
        function setKeystore (all) {
            vm.keystore = {
                file:       all.keystoreReport.keystoreFile,
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
                alias:          all.keystoreReport.privateKeyAlias,
                owner:          all.keystoreReport.certId.name,
                issuer:         "UKNOWN", //@todo: verify the source,
                privateKeyAlias: all.keystoreReport.privateKeyAlias

            }
        }
    }
})();