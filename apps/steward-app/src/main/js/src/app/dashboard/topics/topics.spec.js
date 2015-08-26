describe("topics.js", function () {

    var $location, scope, app, TopicsModelFactory, controller;


    beforeEach(module('stewardApp'));
    beforeEach(module('topics-model'));

    beforeEach(inject(function ($rootScope, $controller ,_$app_, _TopicsModelFactory_) {
        scope                   = $rootScope.$new();
        app                     = _$app_;
        TopicsModelFactory      = _TopicsModelFactory_;
        controller              = $controller;
    }));

    describe("topics-model.js", function () {
        it('App TopicsModelFactory should exist', function () {
            expect(TopicsModelFactory).toBeTruthy();
        });
    });
});

