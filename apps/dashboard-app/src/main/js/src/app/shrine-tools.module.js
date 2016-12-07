(function () {
    'use strict';

    var dependencies = [
        'oc.lazyLoad',
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.modal',
        'shrine.common'/*,
        'shrine.common.authentication'*/
    ];

    // -- create module -- //
    angular
        .module('shrine-tools', dependencies);
})();
