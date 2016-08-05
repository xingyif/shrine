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
        setLoggedOutLayout();
        steward.logout = logout;

        // -- set login callback. todo: investigate advantage of broadcaster instead.  -- //
        StewardService.setLoginSubscriber(loginSubscriber);

        function setLoggedInLayout() {
            steward.layoutWidth = 10;
        }

        function setLoggedOutLayout() {
            steward.layoutWidth = 12;
        }

        function logout() {
            setLoggedOutLayout();
            StewardService.logoutUser();
        }

        function loginSubscriber(appUser) {
            steward.showStewardMenuOptions = steward.isSteward();
            setLoggedInLayout();
        }
    }
})();
