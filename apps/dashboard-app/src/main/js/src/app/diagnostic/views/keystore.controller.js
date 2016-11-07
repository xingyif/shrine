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
        init();


        /**
         *
         */
        function init() {
            $app.model.getKeystore()
                .then(setKeystore, handleFailure);

            $app.model.getQep()
                .then(setQep, handleFailure)
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
                ['Expires',           keystore.expires],
                ['Private Key Alias', keystore.caTrustedAlias],
                ['MD5 Signature',     keystore.md5Signature],
                ['SHA256 Signature',  keystore.sha256Signature]
            ];

            vm.caCertificate = [
                ['Alias',             keystore.caTrustedAlias],
                ['MD5 Signature',     keystore.caTrustedSignature]
            ];


            vm.downStreamValidation = downStreamValidation(keystore);
            vm.hubAndPeerValidation = hubAndPeerValidation(keystore);
            vm.keyStoreContents     = keyStoreContents(keystore)
        }

        function keyStoreContents(keystore) {
            function handleEntry(entry) {
                return [entry.alias, entry.cn, entry.md5]
            }
            return map(handleEntry, keystore.abbreviatedEntries)
        }

        function downStreamValidation(keystore) {
            $log.warn("called");
            var remoteSite = keystore.remoteSiteStatuses[0];
            if (remoteSite.timeOutError) {
                return [['Timed Out', "Timed out while connecting to the hub"]]
            } else {
                return [
                    ["Signature Matches Hub's?", remoteSite.theyHaveMine? "Yes": "No"],
                    ["CA Certificate Matches Hub's?", remoteSite.haveTheirs? "Yes": "No"]
                ]
            }
        }

        function hubAndPeerValidation(keystore) {
            function handleStatus(siteStatus) {
                if (siteStatus.timeOutError) {
                    return [siteStatus.siteAlias, "Timed Out", "N/A"]
                } else {
                    return [siteStatus.siteAlias, siteStatus.theyHaveMine, siteStatus.haveTheirs]
                }
            };

            return map(handleStatus, keystore.remoteSiteStatuses)
        }

        //TODO: Good error handling
        function handleFailure(failure) {
            $log.error(JSON.stringify(failure));
        }
    }
})();