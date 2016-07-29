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
            dateFormatter: service.commonService.dateService.utcToMMDDYYYY,
            prettifyName: prettifyName,
            prettifyContents: prettifyContents
        };

        function prettifyName(name) {
            return (name.length > 50) ? (name.substring(0, 50) + '...') : name;
        }

        function prettifyContents(queryContents) {
            var array = queryContents.split('<');
            var prettifiedContents = array.join('\n' + '\t' + '<');
            return prettifiedContents;
        }

        function isSteward() {
            var role = service.getRole();
            return role === service.constants.roles.dataSteward;
        }
    }
})();