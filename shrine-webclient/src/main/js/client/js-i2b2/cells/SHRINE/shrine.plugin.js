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

    function navigateTo(route) {
        i2b2.CRC.view.status.selectTab('shrine');
        showDisplay(route);
        var pluginLocation = window.frames['shrine-plugin'].window.location;
        pluginLocation.href = pluginLocation.origin + pluginLocation.pathname + '#' + route;
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
            }]
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
        return '<div id="pluginErrorDetail" style="display:none;">' +
            '<div class="hd" style="background:#6677AA;">SHRINE Result Status</div>' +
            '<div class="bd">' +
            '<br />' +
            '<div style="border: 1px solid #C0C0C0; max-height: 450px;' +
            'background-color: #FFFFFF; overflow: scroll; word-wrap: break-word; padding: 10px 5px;"' +
            'id="pluginErrorDetail" class="StatusBoxText">' +
            '<div><b>Summary:</b></div>' +
            '<div>' + data.status + '</div><br/>' +
            '<div><b>Description:</b></div>' +
            '<div><p>' + data.statusMessage + '</p></div><br/>' +
            '</div>' +
            '</div>' +
            '</div>';
    }
})();
