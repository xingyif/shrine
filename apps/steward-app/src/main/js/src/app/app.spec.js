describe("app.js", function () {

    var $location, scope, app;

    beforeEach(module('stewardApp'));

    beforeEach(inject(function ($rootScope, $controller , _$location_, _$app_) {
        $location   = _$location_;
        scope       = $rootScope.$new();
        app         = _$app_;
    }));

    describe("$app Service.", function () {

        it('$app service should be created', function () {
            expect(app).toBeTruthy();
        });

        it('$app.globals should exist', function () {
            expect(app.globals).toBeTruthy();
        });

        it('$app.globals.UserRoles should be initialized', function () {
            expect(app.globals.UserRoles.ROLE1).toBe('Researcher');
            expect(app.globals.UserRoles.ROLE2).toBe('DataSteward');
            expect(app.globals.UserRoles.ROLE3).toBe('Admin');
        });

        it('$app.globals.ViewConfig should be initialized', function () {
            //expect(app.globals.ViewConfig.INDEX).toBe(1);
            //expect(app.globals.ViewConfig.RANGE).toBe(4);
            //expect(app.globals.ViewConfig.LIMIT).toBe(5);
        });
        it('$app.globals.States should be initialized', function () {
            expect(app.globals.States.STATE1).toBe('Pending');
            expect(app.globals.States.STATE2).toBe('Approved');
            expect(app.globals.States.STATE3).toBe('Rejected');
        });

    });

});
