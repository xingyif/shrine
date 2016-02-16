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


    /**
     *  This is a very 'un-angular' way to do this, but for-in behavior is intrinsically un-angular as
     *  directives do not play nicely with for-in object traversal when using the ng-repeat/nested directive
     *  recursive approach.  Trust me...this is most pain-free way to do this.
     * @param json
     * @param spaces
     * @returns {string}
     */
    function buildHtmlFromJson (json, spaces) {

        // -- local vars -- //
        var html    = '';
        var indent  = '&nbsp;&nbsp&nbsp;&nbsp';

        // -- set tab to indicate hierarchy -- //
        spaces      =  spaces || '';
        spaces      += indent;


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

