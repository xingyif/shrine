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
            .each(function(el) {
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
        .each(function(id) {
            $(id).hide();
        })
        /*@ie no fun! .each(id => $(id).hide());*/
    }

    function hideDisplay() {
        $(pluginId).hide(e);
    }

    // -- @todo: pass through messaging to the child frame. -- //
    function navigateTo(route) {
        i2b2.CRC.view.status.selectTab('shrine');
        showDisplay(route);
        var pluginLocation = window.frames['shrine-plugin'].window.location;
        pluginLocation.href = pluginLocation.origin + pluginLocation.pathname + '#' + route;
    }
})();
