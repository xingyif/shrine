(function () {
    'use strict';

    // -- angular module -- //
    angular.module('shrine.common')
        .factory('DataTypesService', DataTypesService);


    function DataTypesService () {

        // -- constants -- //
        var Types = {
            String:     'String',
            Number:     'Number',
            Undefined:  'Undefined',
            Boolean:    'Boolean',
            Array:      'Array',
            Null:       'Null'
        };


        // -- public -- //
        return {
            Types:          Types,
            typeOf:         typeOf,
            isTypeOf:       isTypeOf,
            isArray:        isArray,
            isBoolean:      isBoolean,
            isString:       isString,
            isNumber:       isNumber,
            isUndefined:    isUndefined,
            isNull:         isNull

        };


        // -- private -- //
        /**
         * Returns the type of a primitive or array.
         * @param {Object} an object, string, number, boolean, or array
         * @return{String} a string indicating the type or undefined.
         */
        function typeOf(element){
            return (Object.prototype.toString.call(element))
                .split('[object ')[1]
                .split(']')[0];
        }


        /**
         * Returns true if given element is of the 'type' provided
         * @param element
         * @param type
         * @returns {boolean}
         */
        function isTypeOf(element, type) {
            return typeOf(element) === type;
        }


        /**
         * true if element is an array type.
         * @param element
         * @returns {boolean}
         */
        function isArray(element) {
            return isTypeOf(element, Types.ARRAY);
        }


        /**
         * true if element is a boolean type.
         * @param element
         * @returns {boolean}
         */
        function isBoolean(element) {
            return isTypeOf(element, Types.BOOLEAN);
        }


        /**
         * true if element is a string.
         * @param element
         * @returns {boolean}
         */
        function isString(element) {
            return isTypeOf(element, Types.STRING);
        }


        /**
         * true if element is a number.
         * @param element
         * @returns {boolean}
         */
        function isNumber(element) {
            return isTypeOf(element, Types.NUMBER);
        }


        /**
         * true if element is undefined.
         * @param element
         * @returns {boolean}
         */
        function isUndefined(element) {
            return isTypeOf(element, Types.UNDEFINED);
        }


        /**
         * true if element is null but not undefined.
         * @param element
         * @returns {boolean}
         */
        function isNull(element) {
            return isTypeOf(element, Types.NULL);
        }
    }
})();
