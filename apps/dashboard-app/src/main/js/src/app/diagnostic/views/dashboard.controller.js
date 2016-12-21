/**
 * Controller for the Remote Dashboards panel.
 * Parses the keystore get call, and has to handle
 * some tricky caching logic with the model backend
 * (namely we don't want to reset the dashboard links themselves
 *  between remoteDashboard visits)
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
                if (abbreviatedEntry.url != "") // ignore self
                    tempList.push(abbreviatedEntry)
            }

            vm.otherDashboards = [['Hub', '', true]].concat(map(entryToPair, tempList));
            vm.otherDashboards.sort(comparator);
            vm.clearCache = clearCache;
            vm.switchDashboard = switchDashboard;
        }

        /**
         * Lexicographic sort where Hub is always first. I'm sure there's a more
         * golf-way of writing this.
         */
        function comparator(first, second) {
            if (first[0] == 'Hub') {
                return -2;
            } else if (second[0] == 'Hub') {
                return 2;
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
        }

        function clearCache() {
            $app.model.clearCache();
        }

        function entryToPair(entry){
            return [entry.siteAlias, entry.url, entry.theyHaveMine && entry.haveTheirs && !entry.timeOutError];
        }
    }
})();

