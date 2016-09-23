(function () {
    'use strict';

    // -- angular module -- //
    angular.module('shrine.common')
        .factory('XMLService', XMLService);


    function XMLService() {

        var x2js = new X2JS();

        // -- public -- //
        return {
            xmlStringToJson: xmlStringToJson
        };

        // -- private -- //
        function xmlStringToJson(xmlString) {
            return x2js.xml_str2json(xmlString);
        }

        /**
         * Convert a string representation of an xml document to an XML Document.
         * IE 10+ and all other modernish browser xml doc parser.
         * @see:  http://caniuse.com/#feat=xml-serializer, https://davidwalsh.name/convert-xml-json
         */
        function stringToXML(xmlString) {
            return new window.DOMParser().parseFromString(xmlString, "text/xml");
        }


        /**
         * Traverse an XML Document Object and create a Json Object.
         * @param xml
         * @returns {{}}
         */
        function xmlToJson(xml) {

            // -- local vars -- //
            var item, nodeName,
                jsonObject = {};

            // -- if node is text type -- //
            if (xml.nodeType === 3) {
                jsonObject = xml.nodeValue;
            }

            // -- if node is a parent -- //
            else if (xml.childNodes.length === 0) {
                for (var i = 0; i < xml.childNodes.length; i++) {

                    item = xml.childNodes.item(i);
                    nodeName = item.nodeName;

                    if (jsonObject[nodeName] === undefined) {
                        jsonObject[nodeName] = xmlToJson(item);
                    }

                    else {

                        if (jsonObject[nodeName].push === undefined) {
                            jsonObject[nodeName] = [jsonObject[nodeName]];
                        }

                        jsonObject[nodeName].push(xmlToJson(item));
                    }
                }
            }

            return jsonObject;
        }
    }
})();

