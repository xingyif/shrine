(function () {
    'use strict';

    var dependencies = [
        'oc.lazyLoad',
        'ui.router',
        'ui.bootstrap',
        'angular-loading-bar',
        'ui.bootstrap.modal',
        'shrine.common'/*,
        'shrine.common.authentication'*/
    ];

    // -- create module -- //
    angular
        .module('shrine-tools', dependencies);
})();
