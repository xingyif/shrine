(function () {
    'use strict';

    var dependencies = [
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.modal',
        'angular-loading-bar',
        'shrine.common'
    ];

     angular
        .module('shrine.steward', dependencies);
})();