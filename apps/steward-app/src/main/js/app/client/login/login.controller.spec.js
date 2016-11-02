(function () {
    'use strict';

    describe('shrine.steward.login controller tests', LoginControllerSpec);

    function LoginControllerSpec() {

        // -- vars -- //
        var loginController;

        function setup() {
            module('shrine.steward.login');
            inject(function ($controller) {
                loginController = $controller('LoginController', {});
                loginController.username ='testuser';
                loginController.password = 'testpassword';
            });
        }

        //-- setup --/
        beforeEach(setup);

        it('LoginController should exist', function () {
            expect(typeof (loginController)).toBe('object');
        });

        it('LoginController checkLogin should exist', function () {
            expect(loginController.checkLogin).toBeDefined();
        });
    }
})();