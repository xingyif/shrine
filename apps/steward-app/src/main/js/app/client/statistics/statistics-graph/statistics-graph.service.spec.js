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
        it('getMinUser should exist', function () {
            expect(typeof (statsGraphService.getMinUser)).toBe('function');
        });
        it('getMinUserQueryCount should exist', function () {
            expect(typeof (statsGraphService.getMinUserQueryCount)).toBe('function');
        });
        it('getCountAsPercentage should exist', function () {
            expect(typeof (statsGraphService.getCountAsPercentage)).toBe('function');
        });
        it('formatUserame should exist', function () {
            expect(typeof (statsGraphService.formatUsername)).toBe('function');
        });

        it('formatUsername should truncate benajamindanielcarmen to benjaminda...', function () {
            var result = statsGraphService.formatUsername('benjamindanielcarmen');
            expect(result).toBe('benjaminda...');
        });

        it('formatUsername should not truncate ben', function () {
            var result = statsGraphService.formatUsername('ben');
            expect(result).toBe('ben');
        });

        it('getSortedUsers should work', function () {
            var sortedUsers = statsGraphService.getSortedUsers(users);
            var minUser = sortedUsers[0];
            var maxUser = sortedUsers.reverse()[0];
            expect(minUser._2).toBe(1);
            expect(maxUser._2).toBe(7);
        });

        it('getMinUserQueryCount should work', function () {
            var minUserQueryCount = statsGraphService.getMinUserQueryCount(users);
            expect(minUserQueryCount).toBe(1);
        });

        it('getMaxUserQueryCount should work', function () {
            var maxUserQueryCount = statsGraphService.getMaxUserQueryCount(users);
            expect(maxUserQueryCount).toBe(7);
        });

       it('getCountAsPercentage should work as expected for values over 20%', function () {
           var pct = statsGraphService.getCountAsPercentage(50, 10, 100);
            expect(pct).toBe(50);
        });

        it('getCountAsPercentage should work as expected minimum query values under 20%', function () {
           var pct = statsGraphService.getCountAsPercentage(10, 10, 100);
            expect(pct).toBe(20);
        });

        it('getCountAsPercentage should work as expected for values under 20% but greater than minimum', function () {
           var pct = statsGraphService.getCountAsPercentage(13, 10, 100);
            expect(pct).toBe(23);
        });
    }
})();