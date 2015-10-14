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


    function simulateI2b2Obj() {

        var self = {},
            errorObject = {
                summary:     "SHRINE Failed to Start",
                description: "The SHRINE software is not running at the queried" +
                " site. This error must be corrected at the queried site.Check network status or contact your local SHRINE administrator. For faster assistance, expand this window and provide all text below this line to your local SHRINE administrator.",
                details:     "There is a fatal syntax error in the remote site's" +
                " shrine.conf or another .conf file. The remote site admin should check to make sure that there are no stray/missing quotes or brackets, and that URLs are entered correctly.",
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
            i2b2Obj = simulateI2b2Obj();
            container   = i2b2Obj.dispDIV;
            errorObjects  = [i2b2Obj.errorObject];
        }

        errObjects = errorObjects;

        //this sets up the events.
        anchors     = container.getElementsByClassName('query-error-anchor');

        //something's wrong captain, abandon ship!
        if(!anchors.length|| !errorObjects.length) {
            return;
        }

        addAnchorEvents();

        function expandErrorDetailDiv (ev) {
            btnExpand.style.display   = 'none';
            btnContract.style.display = 'inline';
            $('errorDetailDiv').innerHTML = '<div><b>Name:</b></div><div>' + errorData.summary + '</div><br/>' +
                '<div><b>Description:</b></div><div>' + errorData.description + '</div><br/>' +
                '<div><b>Technical Details:</b></div><pre style="margin-top:0">' + errorData.details + '</pre><br/>' +
                '<div><i>For information on troubleshooting and resolution, check' +
                ' <a href="' + errorData.codec +'" target="_blank">the SHRINE Error' +
                ' Codex</a>.</i></div>';
        }


        function retractErrorDetailDiv (ev) {
            btnExpand.style.display   = 'inline';
            btnContract.style.display = 'none';
            $('errorDetailDiv').innerHTML = '<div><b>Name:</b></div><div>' + errorData.summary + '</div><br/>' +
                '<div><b>Description:</b></div><div>' + errorData.description + '</div>'
        }

        function onClick(event) {

            event.preventDefault();

            errorData = event.currentTarget.__errorData__;

            btnExpand   = document.getElementById('btnExpandErrorDetail');
            btnContract = document.getElementById('btnContractErrorDetail');

            // -- add event listeners for expand and contract as well --//
            btnExpand.addEventListener('click', expandErrorDetailDiv, false);
            btnContract.addEventListener('click', retractErrorDetailDiv, false);

            showErrorDetail(errorData);
        }

        function showErrorDetail(detailObj) {
            var handleCancel = function() {
                this.cancel();
                removeAllEvents();
                retractErrorDetailDiv();
            };

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
                e.preventDefault();
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
            $('errorDetailDiv').innerHTML = '<div><b>Name:</b></div><div>' + errorData.summary+ '</div><br/>' +
                '<div><b>Description:</b></div><div>' + errorData.description + '</div>';
        }

        function addAnchorEvents () {
            var el, length = anchors.length;

            // -- will need to iterate over these once they are created and add event listeners.
            for(var i = 0; i < length; i ++) {
                var el = anchors[i];
                el.__errorData__ = errorObjects[i];
                el.addEventListener('click', onClick, false);
            }
        }

        function removeAllEvents () {
            btnExpand.removeEventListener('click', expandErrorDetailDiv);
            btnContract.removeEventListener('click', retractErrorDetailDiv);
        }
    }


    return EnhancedError;
})();