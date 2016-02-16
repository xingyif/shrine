( function () {


    // -- register directive with angular  -- //
    angular.module('shrine-tools')
        .directive('bootcordion', Bootcordion);


    var compileRef;

    /**
     *
     * @returns {{restict: string, replace: boolean, controller: BootcordionController,
     * controllerAs: string, link: BootcordionLinker, scope: {bootData: string}}}
     * @constructor
     */
    function Bootcordion ($compile) {

        compileRef = $compile;

        return {
            restict: 'E',
            replace: true,
            link: BootcordionLinker,
            controller: BootcordionController,
            controllerAs: 'vm',
            scope: {
                data: '='
            }
        }
    }


    /**
     *
     * @constructor
     */
    BootcordionController.$inject = ['$scope'];
    function BootcordionController ($scope) {
        var vm  = this;
        vm.data = $scope.data
    }


    /**
     *
     * @type {string[]}
     */
    BootcordionLinker.$inject = ['scope', 'element', 'attributes'];
    function BootcordionLinker (s, e, a) {
        var vm      = s.vm;
        var html    = buildHtmlFromJson(vm.data);
        e.append(html);
        compileRef(e.contents())(s);
    }



    function buildHtmlFromJson (json, spaces) {
        spaces      =  spaces || ''
        var html    = '';
        var indent  = '&nbsp;&nbsp&nbsp;&nbsp';
        spaces += indent


        for(var el in json){
            if(json.hasOwnProperty(el)){

                //open tag and append name if it is not an array element.
                if(isNaN(el)){
                    html += '<div>' + el;
                }

                //if value is a leaf.
                if(typeof(json[el]) !== 'object'){
                    html +=  json[el];
                }

                //if value is not a leaf
                else {
                    html += buildHtmlFromJson(json[el], spaces);
                }

                //close tag if it is not an arra element.
                if(isNaN(el)){
                    html += '</div>';
                }
            }
        }

        return html;
    }

})();

