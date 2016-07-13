(function() {
    'use strict';
    angular
        .module('shrine.steward')
        .controller('TopicsController', TopicsController);

    function TopicsController() {

        var topics = this;

        topics.message = 'TopicsController loaded';
    }
})();