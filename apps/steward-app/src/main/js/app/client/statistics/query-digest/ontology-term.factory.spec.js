(function () {
    'use strict';

    describe('Ontology Term Factory Tests', OntologyTermFactorySpec);

    function OntologyTermFactorySpec() {

        var ontTermFactory;

        function setup () {
            module('shrine.steward.statistics');

            inject(function (_OntologyTerm_) {
                ontTermFactory = _OntologyTerm_;
            });
        }

        beforeEach(setup);

        it('Ontology Term Factory should exist', function () {
            expect(typeof(ontTermFactory)).toBe('function');
        });

        it('Ontology Term Factory should return a new OntologyTerm object', function () {
            var ontTerm = new ontTermFactory ();
            expect(typeof(ontTerm)).toBe('object');
        });

    }

})();