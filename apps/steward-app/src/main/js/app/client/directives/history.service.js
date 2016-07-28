(function () {
    'use strict';

    angular.module('shrine.steward')
        .factory('HistoryService', HistoryService);

    HistoryService.$inject = ['StewardService'];
    function HistoryService(service) {

        return {
            isSteward: isSteward,
            getUrl: service.getUrl,
            viewConfig: service.constants.viewConfig,
            dateFormatter: service.commonService.dateService.utcToMMDDYYYY
        };

        function isSteward() {
            var role = service.getRole();
            return role === service.constants.roles.dataSteward;
        }
    }
})();