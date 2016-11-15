/**
 * Created by ty on 11/7/16.
 */
(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools')
        .controller('DashboardController', DashboardController);


    /**
     *
     * @type {string[]}
     */
    DashboardController.$inject = ['$app', '$log', '$location'];
    function DashboardController ($app, $log, $location) {
        var vm = this;
        var map = $app.model.map;
        vm.keyStoreError = false;

        init();

        /**
         *
         */
        function init () {
            $app.model.getKeystore()
                .then(setDashboard, handleFailure);
        }

        function handleFailure (failure) {
            vm.keyStoreError = failure;
        }

        /**
         *
         * @param keystore
         */
        function setDashboard (keystore) {
            var modelStatuses = $app.model.m.remoteSiteStatuses;
            var tempList = [];
            for (var i = 0; i < modelStatuses.length; i++) {
                var abbreviatedEntry = modelStatuses[i];
                if (abbreviatedEntry.url != "")
                    tempList.push(abbreviatedEntry)
            }

            vm.otherDashboards = [['Self', '']].concat(map(entryToPair, tempList));
            vm.clearCache = clearCache;
            vm.switchDashboard = switchDashboard;
        }

        function switchDashboard(url, alias) {
            $app.model.toDashboard.url = url;
            $app.model.m.siteAlias = alias == 'Self'? '': alias;
            clearCache();
            $location.url("/diagnostic/summary");
        }

        function clearCache() {
            for (var member in $app.model.cache) {
                if($app.model.cache.hasOwnProperty(member)) delete $app.model.cache[member];
            }
        }

        function entryToPair(entry){
            return [entry.siteAlias, entry.url];
        }
    }
})();

