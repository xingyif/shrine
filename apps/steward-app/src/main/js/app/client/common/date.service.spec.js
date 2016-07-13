(function () {
    'use strict';

    describe('shine.common DateService tests', DateServiceSpec);

    function DateServiceSpec() {

        // -- vars -- //
        var dateService;
        var utcSeconds = 1451624400000;
        var timestamp = '01/01/2016';

        function setup() {
            module('shrine.common');
            inject(function (DateService) {
                dateService = DateService;
            });
        }

        // -- setup --//
        beforeEach(setup);

        //-- tests -- //
        it('DateService should exist', function () {
            expect(typeof (dateService)).toBe('object');
        });

        it('timestampToUtc should return UTC seconds if given a date string.', function () {

            // -- arrange -- //
            var expectedResult = 1451624400000;

            // -- act -- //
            var result = dateService.timestampToUtc(timestamp);

            // -- assert -- //
            expect(result).toBe(expectedResult);
        });

        it('utcToMMDDYYY should return a date formatted as MM/DD/YYYY if passed UTC seconds', function () {

            // -- arrange -- //
            var expectedResult = '1/1/2016';

            //act
            var result = dateService.utcToMMDDYYYY(utcSeconds);

            //assert
            expect(result).toBe(expectedResult);
        });

        it('utcToTimeStamp should return a time formatted as "MM/DD/YYYY HH:MM:SS"', function () {

            // -- arrange -- //
            var expectedResult = '01/01/2016 0:00:00 AM';

            //act
            var result = dateService.utcToTimeStamp(utcSeconds);

            //assert
            expect(result).toBe(expectedResult);
        });
    }
})();
