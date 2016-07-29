(function () {
    'use strict';

    // -- todo: look at using this service as a shared resource among shrine.steward.topics elements. instead of heavy 
    // -- reliance on interpolation -- //

    angular.module('shrine.steward.topics')
        .factory('TopicsService', TopicsService);

    TopicsService.$inject = ['StewardService'];
    function TopicsService(service) {

        return {
            isSteward: isSteward,
            getUrl: service.getUrl,
            states: service.constants.states,
            viewConfig: service.constants.viewConfig,
            dateFormatter: service.commonService.dateService.utcToMMDDYYYY
        };

        function isSteward() {
            var role = service.getRole();
            return role === service.constants.roles.dataSteward;
        }

    }
})();