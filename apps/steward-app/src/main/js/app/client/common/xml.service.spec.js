(function () {
    'use strict';

    describe('shrine.common XMLService tests', XMLServieSpec);
    function XMLServieSpec() {

        // -- vars -- //
        var xmlService;

        function setup() {
            module('shrine.common');
            inject(function (XMLService) {
                xmlService = XMLService;
            });
        }

        // -- setup -- //
        beforeEach(setup);

        // -- tests -- //
        it('CommonService should exist', function () {
            expect(typeof (xmlService)).toBe('object');
        });

        it('xmlStringToJson should convert an simple xml string to json', function () {

            // -- arrange -- //
            var xmlString = '<person><name>Test</name></person>';
            var expectedResult = {
                person: {
                    name: 'Test'
                }
            };
            // -- act -- //
            var result = xmlService.xmlStringToJson(xmlString);

            // -- assert --//
            expect(result.name).toBe(expectedResult.name);
        })

    }
})();