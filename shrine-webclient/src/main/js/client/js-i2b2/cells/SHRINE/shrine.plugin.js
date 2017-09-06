/**
 * @todo: This logic could be designed to handle all views.
 */

(function () {
    'use strict';
    var pluginId = 'shrinePlugin';
    //var tabId = 'shrineTab';
    var contentIds = [
        'infoQueryStatusText',
        'infoQueryStatusChart',
        'infoQueryStatusReport',
        'infoDownloadStatusData',
        pluginId
    ];

    // -- add plugin to i2b2 namespace -- //
    i2b2.SHRINE.plugin = new i2b2Base_cellViewController(i2b2.SHRINE, pluginId);
    i2b2.SHRINE.plugin.showDisplay = showDisplay;
    i2b2.SHRINE.plugin.hideDisplay = hideDisplay;
    i2b2.SHRINE.plugin.ZoomView = zoomView;
    i2b2.SHRINE.plugin.navigateTo = navigateTo;
    i2b2.SHRINE.plugin.errorDetail = errorDetail;


    function zoomView() {
        const height = jQuery('#infoQueryStatusText').css('height');
        jQuery('#shrinePlugin').css('height', height);
    }

    function showDisplay(route) {
        clearAllTabs();
        setShrineTabActive(route);
        hideContent();
        $(pluginId).show();
    }

    function clearAllTabs() {
        $(pluginId).parentNode.parentNode
            .select('DIV.tabBox.active')
            .each(function (el) {
                el.removeClassName('active');
            });
        /* IE is no fun!  .each(el => el.removeClassName('active'));*/
    }

    function setShrineTabActive(route) {
        // set us as active
        $(pluginId)
            .parentNode
            .parentNode
            .select('DIV.tabBox.' + route)[0]
            .addClassName('active');
    }

    function hideContent() {
        contentIds
            .each(function (id) {
                $(id).hide();
            })
        /*@ie no fun! .each(id => $(id).hide());*/
    }

    function hideDisplay() {
        $(pluginId).hide(e);
    }


    function mailTo() {
        i2b2.CRC.view.status.selectTab('shrine');
    }

    function queryViewer() {
        i2b2.CRC.view.status.selectTab('shrine');
    }

    
    
    function navigateTo(route) {
        i2b2.CRC.view.status.selectTab('shrine');
        showDisplay(route);
        window.frames['shrine-plugin'].window.postMessage(route, '*');
    }


    function errorDetail(data) {

        var j$ = jQuery;

        j$('body').append(j$(getDialogHTML(data)))

        var pluginErrorDetail = new YAHOO.widget.SimpleDialog("pluginErrorDetail", {
            width: "820px",
            fixedcenter: true,
            constraintoviewport: true,
            modal: true,
            zindex: 700,
            buttons: [{
                text: "Done",
                handler: function() {this.cancel();},
                isDefault: true
            }]11
        });

        //$ = prototype.js
        $('pluginErrorDetail').show();
        pluginErrorDetail.validate = function () {
            return true;
        };

        pluginErrorDetail.render(document.body);

        // / display the dialoge
        pluginErrorDetail.center();
        pluginErrorDetail.show();
    }

    // ES5 :(
    function getDialogHTML(data) {
        return 
            '<div id="pluginErrorDetail" style="display:none;">' +
            '<div class="hd" style="background:#6677AA;">SHRINE Result Status</div>' +
            '<div class="bd">' +
            '<br />' +
            '<button>More Detail </button>' +
            '<button style="display: none">Less Detail</button>' +
            '<div style="border: 1px solid #C0C0C0; max-height: 450px;' +
            'background-color: #FFFFFF; overflow: scroll; word-wrap: break-word; padding: 10px 5px;"' +
            'id="pluginErrorDetail" class="StatusBoxText">' +
            '<div><b>Summary:</b></div>' +
            '<div>' + data.status + '</div><br/>' +
            '<div><b>Description:</b></div>' +
            '<div><p>' + data.statusMessage + '</p></div><br/>' +
            '<span id="pluginMoreDetail" style="display:none">' + 
                '<div><b>Codec:</b></div>' +
                '<div>' + data.problemDigest && data.problemDigest.codec? data.problemDigest.codec : 'not available' + '</div><br/>' +
                '<div><b>Stamp:</b></div>' +
                '<div>' + data.problemDigest && data.problemDigest.stampText? data.problemDigest.stampText : 'not available' + '</div><br/>' +
                '<div><b>Stack Trace Name:</b></div>' +
                '<div>' + data.problemDigest && data.problemDigest.codec? data.problemDigest.codec : 'not available' + '</div><br/>' +
                '<div><b>Stack Trace Message:</b></div>' +
                '<div>' + data.problemDigest && data.problemDigest.description? data.problemDigest.description : 'not available' + '</div><br/>' +
                '<div><b>Stack Trace Details:</b></div>' +
                '<div>' + data.problemDigest && data.problemDigest.detailsString? data.problemDigest.detailsString.split(',').join(',\n') : 'not available' + '</div><br/>' +
            '</span>'
            '</div>' +
            '</div>' +
            '</div>';
    }
})();

<div id="dialogErrorDetail" style="display:none;">
    <div class="hd" style="background:#6677AA;">Query Error Detail</div>
    <div class="bd">
        <br />
        <button id='btnExpandErrorDetail'>More
            Detail
        </button>
        <button id='btnContractErrorDetail'
                style='display: none'>Less Detail
        </button>
        <a id="dlgNetworkStatus"
           style="cursor: pointer; position:absolute; right:20px;
               top: 58px; text-decoration:
               underline; color:blue; display: none">Network
            Status</a>
        <div style="border: 1px solid #C0C0C0; max-height: 450px;
        background-color: #FFFFFF; overflow: scroll; word-wrap: break-word; padding: 10px 5px;"
             id="errorDetailDiv" class="StatusBoxText">
        </div>
    </div>
</div>

