(function () {
    'use strict';

    var dependencies = [
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.modal',
        'angular-loading-bar',
        'shrine.common',
        'shrine.steward.login'
    ];

     angular
        .module('shrine.steward', dependencies);
})();