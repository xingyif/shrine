(function () {

    angular.module('shrine.steward')
        .controller('StewardController', StewardController);


    StewardController.$inject = ['CommonService'];
    function StewardController(CommonService) {
                
        var steward = this;
        steward.message = 'StewardController loaded';
        steward.commonService = CommonService;
    }
})();
