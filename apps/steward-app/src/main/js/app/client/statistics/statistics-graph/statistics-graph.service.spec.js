(function () {
    'use strict';

    describe('Statistics Graph Service Tests', StatisticsGraphServiceSpec);

    function StatisticsGraphServiceSpec() {

        var statsGraphService;
        var users = [
            {_2: 7},
            {_2: 1},
            {_2: 3},
            {_2: 2}
        ];

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
        it('formatUserame should exist', function () {
            expect(typeof (statsGraphService.formatuserName)).toBe('function');
        });

        it('formatuserName should truncate benajamindanielcarmen to benjaminda...', function () {
            var result = statsGraphService.formatuserName('benjamindanielcarmen');
            expect(result).toBe('benjaminda...');
        });

        it('formatuserName should not truncate ben', function () {
            var result = statsGraphService.formatuserName('ben');
            expect(result).toBe('ben');
        });

        it('getSortedUsers should work', function () {
            var sortedUsers = statsGraphService.getSortedUsers(users);
            var maxUser = sortedUsers[0];
            expect(maxUser._2).toBe(7);
        });


        it('getMaxUserQueryCount should work', function () {
            var maxUserQueryCount = statsGraphService.getMaxUserQueryCount(users);
            expect(maxUserQueryCount).toBe(7);
        });

       it('getCountAsPercentage should work as expected for values over 20%', function () {
           var pct = statsGraphService.getCountAsPercentage(50, 100);
            expect(pct).toBe(45);
        });

        it('getCountAsPercentage should work as expected minimum query values under 20%', function () {
           var pct = statsGraphService.getCountAsPercentage(10, 100);
            expect(pct).toBe(25);
        });

        it('getCountAsPercentage should work as expected for values under 20% but greater than minimum', function () {
           var pct = statsGraphService.getCountAsPercentage(13, 100);
            expect(pct).toBe(26.5);
        });
    }
})();