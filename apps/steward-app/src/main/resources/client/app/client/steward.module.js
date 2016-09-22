(function () {
    'use strict';

    var dependencies = [
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.modal',
        'angular-loading-bar',
        'shrine.common',
        'shrine.steward.login',
        'shrine.steward.topics',
        'shrine.steward.history',
        'shrine.steward.statistics'
    ];

     angular
        .module('shrine.steward', dependencies);
})();