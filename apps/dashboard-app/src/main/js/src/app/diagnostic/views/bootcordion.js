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
            link:       BootcordionLinker,
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


    //https://jsfiddle.net/GpdgF/5535/

    /**
     *
     * @type {string[]}
     */
    BootcordionLinker.$inject = ['scope', 'element', 'attributes'];
    function BootcordionLinker (s, e, a) {
        var vm      = s.vm;
        var htmlStart = '<div class="tree well" style="height: 600px; overflow: auto; background: transparent"> <ul>';
        var htmlEnd   = '</ul> </div>';

        vm.testClick = function ($event, $element) {
            var children = $($event.target).parent().find(' > ul > li ');
            if (children.is(":visible")) {
                children.hide('fast');

            } else {
                children.show('fast');

            }
        }

        var html    = buildHtmlFromJson(preProcessJson(vm.data['configMap']));
        e.append(htmlStart + html + htmlEnd);
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
    function buildHtmlFromJson (json) {

        // -- local vars -- //
        var html    = '';
        var indent  = '&nbsp;&nbsp&nbsp;&nbsp';
        var sortedKeys = [];
        for (var k in json) {
            if (json.hasOwnProperty(k)) {
                sortedKeys.push(k)
            }
        }
        sortedKeys.sort();
        for(var i = 0; i < sortedKeys.length; i++){
            var el = sortedKeys[i];
            var openingTag = '<ul>',
                closingTag = '</ul>'

            if(json.hasOwnProperty(el)){

                //open tag and append name if it is not an array element.

                if(isNaN(el)){

                    if(isPrimitive(json[el])) {
                        openingTag = '<label>';
                        closingTag = '</label>'
                    }

                    html += '<li><span ng-click="vm.testClick($event, $element)">' + el + '</span>' + openingTag;

                }


                //if value is a leaf.
                if(typeof(json[el]) !== 'object'){
                    html += indent + json[el];
                }

                //if value is not a leaf
                else {
                    html += buildHtmlFromJson(json[el]);
                }

                //close tag if it is not an array element.
                if(isNaN(el)){
                    html += closingTag;
                }
            }
        }

        return html;
    }

    function preProcessJson (object) {
        var result = {};
        for (key in object) {
            if (object.hasOwnProperty(key)) {
                if (!(key.includes("."))) {
                    result[key] = object[key]
                } else {
                    var split = key.split(".");
                    var prev = result;
                    for (var i = 0; i < split.length; i++) {
                        var cur = split[i];
                        if (!(cur in prev)) {
                            prev[cur] = {}
                        }
                        if (i == split.length - 1) {
                            prev[cur] = object[key];
                        } else {
                            prev = prev[cur]
                        }
                    }
                }
            }
        }
        return result;
    }

    function isPrimitive (element) {
        return typeof(element) !== 'object'
    }

})();


/*
 <div class="tree well">
 <ul>
 <li>
 <span><i class="icon-folder-open"></i> Parent</span> <a href="">Goes somewhere</a>
 <ul>
 <li>
 <span><i class="icon-minus-sign"></i> Child</span> <a href="">Goes somewhere</a>
 <ul>
 <li>
 <span><i class="icon-leaf"></i> Grand Child</span> <a href="">Goes somewhere</a>
 </li>
 </ul>
 </li>
 <li>
 <span><i class="icon-minus-sign"></i> Child</span> <a href="">Goes somewhere</a>
 </li>
 </ul>
 </li>
 <li>
 <span><i class="icon-folder-open"></i> Parent2</span> <a href="">Goes somewhere</a>
 <ul>
 <li>
 <span><i class="icon-leaf"></i> Child</span> <a href="">Goes somewhere</a>
 </li>
 </ul>
 </li>
 </ul>
 </div>
 */
