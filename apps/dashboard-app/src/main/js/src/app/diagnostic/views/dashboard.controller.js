/**
 * Created by ty on 11/7/16.
 */
(function () {
    'use strict';

    // -- register controller with angular -- //
    angular.module('shrine-tools', ['ngRoute'])
        .controller('DashboardController', DashboardController);


    /**
     *
     * @type {string[]}
     */
    DashboardController.$inject = ['$app', '$log', '$location', '$route'];
    function DashboardController ($app, $log, $location, $route) {
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

            vm.otherDashboards = [['Hub', '']].concat(map(entryToPair, tempList));
            vm.otherDashboards.sort(comparator);
            vm.clearCache = clearCache;
            vm.switchDashboard = switchDashboard;
        }

        function comparator(first, second) {
            if (first[0] == 'Hub') {
                return -2;
            } else {
                var less = first[0].toLowerCase() < second[0].toLowerCase();
                var eq = first[0].toLowerCase() == second[0].toLowerCase();
                return less? -1: eq? 0 : 1
            }
        }

        //todo remove duplication with header.js
        function switchDashboard(url, alias) {
            $app.model.toDashboard.url = url;
            $app.model.m.siteAlias = alias == 'Hub'? '': alias;
            clearCache();
            $location.url("/diagnostic/summary");
            $route.reload();
        }

        function clearCache() {
            $app.model.clearCache();
        }

        function entryToPair(entry){
            return [entry.siteAlias, entry.url];
        }
    }
})();

