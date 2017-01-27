/**
 * Created by ben on 10/13/15.
 */
var $hrine = window.$hrine = {};
$hrine.EnhancedError =

    (function(){

        var EnhancedError = {},
            config        = config || {
                    //@TODO:
                };


        /**
         *
         * @returns {{}}
         */
        function simulateI2b2Obj() {

            var self = {};
            var errorObject = {
                summary:     "SHRINE Failed to Start",

                description: "The SHRINE software is not running at the queried site. This error must be corrected at the queried site." +
                             "Check network status or contact your local SHRINE administrator. " +
                             "For faster assistance, expand this window and provide all text below this line to your local SHRINE administrator.",

                details:     "There is a fatal syntax error in the remote site's shrine.conf or another .conf file. " +
                             "The remote site admin should check to make sure that there are no stray/missing quotes or brackets, " +
                             "and that URLs are entered correctly.",

                codec:       ""
            };

            self.errorObject = errorObject;


            self.dispDIV = document.getElementById('infoQueryStatusText');

            self.dispDIV.innerHTML =
                '<div style="clear:both;"><div style="float:left; font-weight:bold">SHRINE Critical Error</div></div>';

            //which hospital
            self.dispDIV.innerHTML +=
                '<div style="clear:both;"><br/><div style="float:left; font-weight:bold; margin-left:20px;"> Error Summary: </div>';

            self.dispDIV.innerHTML += "<span title='" + errorObject.summary + "'><b><a class='query-error-anchor' href='#' style='color:#ff0000'>" +
                "<b><span color='#ff0000'>" + errorObject.summary + "</span></b></a></b></span>";

            return self;
        }

        /**
         *  Scope for error dialog.
         */
        EnhancedError.createErrorDialogue =  function (container, errorObjects) {

            var anchors, btnExpand, btnContract, errorData, i2b2Obj;

            //default error.
            if(!container || !errorObjects) {
                i2b2Obj         = simulateI2b2Obj();
                container       = i2b2Obj.dispDIV;
                errorObjects    = [i2b2Obj.errorObject];
            }

            //this sets up the events.
            anchors     = container.getElementsByClassName('query-error-anchor');

            //something's wrong captain, abandon ship!
            if(!anchors.length|| !errorObjects.length) {
                return;
            }

            addAnchorEvents();

            function expandErrorDetailDiv (ev) {
                var errorDetailDiv          = $('errorDetailDiv');
                btnExpand.style.display     = 'none';
                btnContract.style.display   = 'inline';
                errorDetailDiv.innerHTML    = getExpandedHtml();
            }


            function retractErrorDetailDiv (ev) {
                var errorDetailDiv          = $('errorDetailDiv');
                btnExpand.style.display     = 'inline';
                btnContract.style.display   = 'none';
                errorDetailDiv.innerHTML    = getRetractedHtml();
            }

            function onClick(evt) {
                //ie logic.
                var currentTarget = (evt.currentTarget !== undefined)?
                    evt.currentTarget : evt.srcElement.parentElement.parentElement;
                errorData   = currentTarget.__errorData__;
                btnExpand   = document.getElementById('btnExpandErrorDetail');
                btnContract = document.getElementById('btnContractErrorDetail');

                // -- add event listeners for expand and contract as well --//
                addEventListener(btnExpand, 'click', expandErrorDetailDiv, false);
                addEventListener(btnContract,'click', retractErrorDetailDiv, false);

                showErrorDetail(errorData);
            }

            /**
             *
             * @param errorData
             * @returns {string}
             */
            function getRetractedHtml () {

                var wikiBaseUrl = (i2b2.hive.cfg.wikiBaseUrl || 'https://open.med.harvard.edu/wiki/display/SHRINE/');

                if(wikiBaseUrl.lastIndexOf('/') !== wikiBaseUrl.length -1){
                    wikiBaseUrl += '/';
                }

                var retractedHtml = '<div><b>Summary:</b></div>'+
                    '<div>' + errorData.summary + '</div><br/>' +
                    '<div><b>Description:</b></div>'+
                    '<div>' + errorData.description + '</div><br/>' +
                    '<div><i>For information on troubleshooting and resolution, check' +
                    ' <a href="' + wikiBaseUrl + errorData.codec +'" target="_blank">the SHRINE Error' +
                    ' Codex</a>.</i></div>';
                return retractedHtml;
            }

            /**
             *
             * @param errorData
             * @returns {string}
             */
            function getExpandedHtml () {
                var expandedHtml  = getRetractedHtml() +
                    '<br/>' +
                    '<div><b><i>Copy the text below and paste it in an email to your site administrator for a faster response.</i></b></div>' +
                    '<br/>' +
                    '<div><b>Technical Details:</b></div><pre style="margin-top:0">' + errorData.details + '</pre><br/>' +
                    '<div><b>Codec:</b></div><pre style="margin-top:0">' + errorData.codec + '</pre><br/>' +
                    '<div><b>Stamp:</b></div><pre style="margin-top:0">' + errorData.stamp + '</pre><br/>' +
                    '<div><b>Stack Trace Name:</b></div><pre style="margin-top:0">' + errorData.exception.name + '</pre><br/>' +
                    '<div><b>Stack Trace Message:</b></div><pre style="margin-top:0">' + errorData.exception.message + '</pre><br/>' +
                    '<div><b>Stack Trace Details:</b></div><pre style="margin-top:0">' + errorData.exception.stackTrace + '</pre><br/>';
                return expandedHtml;
            }

            /**
             *
             * @param detailObj
             */
            function showErrorDetail(detailObj) {

                var handleCancel = function() {
                    this.cancel();
                    removeAllEvents();
                    retractErrorDetailDiv();
                }

                var dialogErrorDetail = new YAHOO.widget.SimpleDialog("dialogErrorDetail", {
                    width: "820px",
                    fixedcenter: true,
                    constraintoviewport: true,
                    modal: true,
                    zindex: 700,
                    buttons: [ {
                        text: "Done",
                        handler: handleCancel,
                        isDefault: true
                    }]
                });

                dialogErrorDetail._doClose = function (e) {
                    this.cancel();
                    removeAllEvents();
                    retractErrorDetailDiv();
                }

                $('dialogErrorDetail').show();
                dialogErrorDetail.validate = function(){
                    return true;
                };
                dialogErrorDetail.render(document.body);

                // / display the dialoge
                dialogErrorDetail.center();
                dialogErrorDetail.show();

                $('errorDetailDiv').innerHTML = getRetractedHtml();
            }


            function addAnchorEvents () {
                var el, length = anchors.length;

                // -- will need to iterate over these once they are created and add event listeners.
                for(var i = 0; i < length; i ++) {
                    var el = anchors[i];
                    el.__errorData__ = errorObjects[i];
                    addEventListener(el, 'click', onClick, false);
                }
            }

            function removeAllEvents () {
                removeEventListener(btnExpand, 'click', expandErrorDetailDiv);
                removeEventListener(btnContract, 'click', retractErrorDetailDiv);
            }
        }

        /**
         * Parse problem node.
         * @param qriNode
         * @returns {{exception: {}}}
         */
        EnhancedError.parseProblem = function (qriNode) {
            var details;
            var problem = {
                    exception: {}
            };

            problem.codec       = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/codec');
            problem.summary     = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/summary');
            problem.description = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/description');
            problem.stamp       = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/stamp');

            //unescape embedded html.
            details = i2b2.h.XPath(qriNode, 'descendant-or-self::query_status_type/problem/details')

            //funky stuff goin' on...get outta here!
            if(!details.length) {
                problem.exception.name  = problem.exception.message = problem.stackTrace =
                    problem.codec = problem.summary  =  'An unexpected error has occurred.';
            }
            //error format as expected.
            else{
                var innerHTML                   = (details[0].xml !== undefined)?
                    details[0].xml : details[0].innerHTML?details[0].innerHTML:null;
				if(!innerHTML)
				{
					innerHTML = jQuery(details[0]).text();
				}
                problem.details                 = innerHTML.unescapeHTML().replace(/(<([^>]+)>)/ig,"");
                problem.exception.name          = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/details/exception/name');
                problem.exception.message       = grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/problem/details/exception/message');
                problem.exception.stackTrace    = parseErrorException(qriNode);
            }

            return problem;
        }


        /**
         * Replace all <line> <message> <exception> and <stacktrace> with <br/> tags.
         * @param node
         * @returns {*}
         */
        function parseErrorException(node) {

            var innerHTML = (node.xml !== undefined)? node.xml : node.innerHTML?node.innerHTML:null;;

			if(!innerHTML)
			{
				innerHTML = jQuery(node).text();
			}
            //no exception, abandon ship!
            if(innerHTML.indexOf('<exception>') == -1){
                return '';
            }

            var content, startIdx, endIdx;

            //fish out the problem section.
            content = innerHTML.split('<problem>')
                .join()
                .split('</problem>')
                .join();

            //fish out the first stack trace.
            startIdx = content.indexOf('<stacktrace>') + 12;
            endIdx   = content.indexOf('</stacktrace>');
            content  = content.substring(startIdx, endIdx);

            //remove all line tags and replace with line break.
            content = content.split('<line>')
                .join('</br>')
                .split('</line>')
                .join()

                //remove all exception tags
                .split('<exception>')
                .join('<br/>')
                .split('</exception>')
                .join()

                //remove all stacktrace tags
                .split('<stacktrace>')
                .join('<br/>')
                .split('</stacktrace>')
                .join()

                //remove all message tags.
                .split('<message>')
                .join('<br/>')
                .split('</message>')
                .join();

            return content;
        }

        /**
         * Grab data for node, return empty string if none.
         * @param node
         * @param xPathString
         * @returns {string}
         */
        function grabXmlNodeData(node, xPathString){
            var nodeVal = i2b2.h.XPath(node, xPathString);
            return (nodeVal.length)? nodeVal[0].firstChild.nodeValue : '';
        }

        /**
         *
         * @param el
         * @param event
         * @param callback
         */
        function addEventListener(el, event, callback) {

            if(el.addEventListener !== undefined) {
                el.addEventListener(event, callback, false);
            } else {
                el.attachEvent('on' + event, callback)
            }
        }

        /**
         *
         * @param el
         * @param event
         * @param callback
         */
        function removeEventListener(el, event, callback) {
            if(el.removeEventListener !== undefined) {
                el.removeEventListener(event, callback);
            } else {
                el.detachEvent('on' + event, callback);
            }
        }


        return EnhancedError;
    })();