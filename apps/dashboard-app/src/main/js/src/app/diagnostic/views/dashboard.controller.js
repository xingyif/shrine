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
    DashboardController.$inject = ['$app', '$log'];
    function DashboardController ($app, $log) {
        var vm = this;
        var map = $app.model.map;

        init();

        /**
         *
         */
        function init () {
            $app.model.getKeystore()
                .then(setDashboard, handleFailure);
        }

        function handleFailure (failure) {
            //TODO: HANDLE FAILURE
            $log.error(JSON.stringify(failure));
        }

        /**
         *
         * @param keystore
         */
        function setDashboard (keystore) {
            var tempList = [];
            for (var i = 0; i < keystore.remoteSiteStatuses.length; i++) {
                var abbreviatedEntry = keystore.remoteSiteStatuses[i];
                if (abbreviatedEntry.url != "")
                    tempList.push(abbreviatedEntry)
            }
            vm.otherDashboards = [['Self', '']];
            if (!(keystore.trustModelIsHub && !keystore.isHub))
                vm.otherDashboards = vm.otherDashboards.concat(map(function(entry){return [entry.siteAlias, entry.url]}, tempList));
            $log.warn(JSON.stringify(vm.otherDashboards));
            vm.clearCache = clearCache;
            vm.switchDashboard = switchDashboard;
        }

        function switchDashboard(url, alias) {
            $app.model.toDashboard.url = url;
            $app.model.m.siteAlias = alias;
            clearCache();
        }

        function clearCache() {
            for (var member in $app.model.cache) {
                if($app.model.cache.hasOwnProperty(member)) delete $app.model.cache[member];
            }
        }
    }
})();

