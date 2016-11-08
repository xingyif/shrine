(function () {
    'use strict';

    describe('Statistics Graph Service Tests', StatisticsGraphServiceSpec);

    function StatisticsGraphServiceSpec() {

        var statsGraphService;

        function setup() {
            module('shrine.steward.statistics');

            inject(function (_StatisticsGraphService_) {
                statsGraphService = _StatisticsGraphService_;
            });
        }

        beforeEach(setup);

        it('Statistics Graph Service should exist', function () {
            expect(typeof (statsGraphService)).toBe('object');
        });

        it('getSortedUsers should exist', function () {
            expect(typeof (statsGraphService.getSortedUsers)).toBe('function');
        });

        it('getMaxUser should exist', function () {
            expect(typeof (statsGraphService.getMaxUser)).toBe('function');
        });
        it('getMaxUserQueryCount should exist', function () {
            expect(typeof (statsGraphService.getMaxUserQueryCount)).toBe('function');
        });
        it('getCountAsPercentage should exist', function () {
            expect(typeof (statsGraphService.getCountAsPercentage)).toBe('function');
        });
    }
})();