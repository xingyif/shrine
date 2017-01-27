/**
 * @todo: This logic could be designed to handle all views.
 */

(function () {
    'use strict';
    var pluginId = 'shrinePlugin';
    var tabId = 'shrineTab';
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


    function zoomView() {
        const height = jQuery('#infoQueryStatusText').css('height');
        jQuery('#shrinePlugin').css('height', height);
    }

    function showDisplay() {
        clearAllTabs();
        setShrineTabActive();
        hideContent();
        $(pluginId).show();
    }

    function clearAllTabs() {
        $(pluginId).parentNode.parentNode
            .select('DIV.tabBox.active')
            .each(el => el.removeClassName('active'));
    }

    function setShrineTabActive() {
        // set us as active
        $(pluginId)
            .parentNode
            .parentNode
            .select('DIV.tabBox.' + tabId)[0]
            .addClassName('active');
    }

    function hideContent() {
        contentIds.each(id => $(id).hide());
    }

    function hideDisplay() {
        $(pluginId).hide();
    }
})();
