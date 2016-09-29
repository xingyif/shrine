(function () {
    'use strict';

    angular
        .module('shrine.common')
        .service('TypeService', TypeService);

    function TypeService() {
        // -- constants -- //
        var types = {
            string: 'String',
            number: 'Number',
            undefined: 'Undefined',
            boolean: 'Boolean',
            array: 'Array',
            null: 'Null',
            object: 'Object'
        };

        return {
            types: types,
            typeOf: typeOf
        };

        /**
         * Returns the type of a primitive or array.
         * @param {Object} an object, string, number, boolean, or array
         * @return{String} a string indicating the type or undefined.
         */
        function typeOf(element) {
            return (Object.prototype.toString.call(element))
                .split('[object ')[1]
                .split(']')[0];
        }
    }
})();
