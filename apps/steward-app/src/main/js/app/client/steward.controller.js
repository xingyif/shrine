(function () {

    angular.module('shrine.steward')
        .controller('StewardController', StewardController);


    StewardController.$inject = ['StewardService'];
    function StewardController(StewardService) {
                
        var steward = this;
        steward.stewardService = StewardService;
        steward.isDevMode = StewardService.constants.isDevMode;

    }
})();
