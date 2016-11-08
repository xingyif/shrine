(function () {
    'use strict';

    describe('Term Service Tests', TermServiceSpec);

    function TermServiceSpec() {

        var termService;

        function setup () {
            module('shrine.steward.statistics');

            inject(function (_TermService_) {
                termService = _TermService_;
            });
        }

        beforeEach(setup);

        it('Term Service should Exist', function () {
            expect(typeof(termService)).toBe('object');
        });

        it('getPct should Exist', function () {
            expect(typeof(termService.getPct)).toBe('function');
        });

        it('getStyleByFrequency should Exist', function () {
            expect(typeof(termService.getStyleByFrequency)).toBe('function');
        });
    }
})();
