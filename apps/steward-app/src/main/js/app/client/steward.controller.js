(function () {

    angular.module('shrine.steward')
        .controller('StewardController', StewardController);


    StewardController.$inject = ['$location','StewardService'];
    function StewardController($location, StewardService) {
        var steward = this;
        steward.isUserLoggedIn = StewardService.isUserLoggedIn;
        steward.getUsername = StewardService.getUsername;
        steward.getRole = StewardService.getRole;
        steward.isSteward = StewardService.isSteward;
        steward.showStewardMenuOptions = false;
        steward.logout = logout;

        // -- set login callback. todo: investigate advantage of broadcaster instead.  -- //
        StewardService.setLoginSubscriber(loginSubscriber);


        function logout() {
            StewardService.logoutUser();
        }

        function loginSubscriber(appUser) {
            steward.showStewardMenuOptions = steward.isSteward();
        }
    }
})();
