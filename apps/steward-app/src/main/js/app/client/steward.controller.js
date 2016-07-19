(function () {

    angular.module('shrine.steward')
        .controller('StewardController', StewardController);


    StewardController.$inject = ['StewardService'];
    function StewardController(StewardService) {
        var steward = this;
        steward.isUserLoggedIn = StewardService.isUserLoggedIn;
        steward.getUsername = StewardService.getUsername;
        steward.getRole = StewardService.getRole;
    }
})();
