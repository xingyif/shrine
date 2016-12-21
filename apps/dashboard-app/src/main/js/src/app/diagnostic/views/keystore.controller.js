(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('KeystoreController', KeystoreController);


    /**
     *
     * @type {string[]}
     */
    KeystoreController.$inject = ['$app', '$log'];
    function KeystoreController ($app, $log) {
        var vm = this;
        var map = $app.model.map;
        vm.qepError = false;
        vm.keyStoreError = false;
        init();


        /**
         *
         */
        function init() {
            $app.model.getKeystore()
                .then(setKeystore, handleKeyStoreFailure);

            $app.model.getQep()
                .then(setQep, handleQepFailure)
        }

        function setQep(qep) {
            vm.trustModelIsHub = qep.trustModelIsHub
        }

        /**
         *
         * @param keystore
         */
        function setKeystore (keystore) {
            vm.isHub = keystore.isHub;
            vm.keystore = {
                file:       keystore.fileName,
                password:   "REDACTED"
            };
            vm.certificate = [
                ['Alias',             keystore.privateKeyAlias],
                ['Owner',             keystore.owner],
                ['Issuer',            keystore.issuer],
                ['Expires',           $app.model.formatDate(new Date(keystore.expires))],
                ['MD5 Signature',     keystore.md5Signature],
                ['SHA256 Signature',  keystore.sha256Signature]
            ];

            vm.caCertificate = [
                ['Alias',             keystore.caTrustedAlias],
                ['MD5 Signature',     keystore.caTrustedSignature]
            ];


            vm.downStreamValidation = downStreamValidation(keystore);
            vm.peerCertValidation   = peerCertValidation(keystore);
            vm.hubCertValidation    = hubCertValidation(keystore);
            vm.keyStoreContents     = keyStoreContents(keystore)
        }

        function keyStoreContents(keystore) {
            function handleEntry(entry) {
                return [entry.alias, entry.cn, entry.md5]
            }
            return map(handleEntry, keystore.abbreviatedEntries)
        }

        function downStreamValidation(keystore) {
            if (keystore.remoteSiteStatuses.length == 0) {
                return [];
            }
            var remoteSite = keystore.remoteSiteStatuses[0];
            if (remoteSite.timeOutError) {
                return [['Timed Out', "Timed out while connecting to the hub"]]
            } else {
                return [
                    ["CA Certificate Matches Hub's?", remoteSite.haveTheirs? "Yes": "No"]
                ]
            }
        }

        function hubCertValidation(keystore) {
            function handleStatus(siteStatus) {
                if (siteStatus.timeOutError) {
                    return [siteStatus.siteAlias, "Timed Out"]
                } else {
                    return [siteStatus.siteAlias, siteStatus.theyHaveMine]
                }
            }

            return map(handleStatus, keystore.remoteSiteStatuses)
        }

        function peerCertValidation(keystore) {
            function handleStatus(siteStatus) {
                if (siteStatus.timeOutError) {
                    return [siteStatus.siteAlias, "Timed Out", "N/A"]
                } else {
                    return [siteStatus.siteAlias, siteStatus.theyHaveMine, siteStatus.haveTheirs]
                }
            }
            return map(handleStatus, keystore.remoteSiteStatuses)
        }

        function handleKeyStoreFailure(failure) {
            vm.keyStoreError = failure;
        }

        function handleQepFailure(failure) {
            vm.qepError = failure;
        }
    }
})();