/**
 * @todo: This logic could be designed to handle all views.
 */

(function () {
    'use strict';
    var pluginId = 'shrinePlugin';
    i2b2.SHRINE.plugin = new i2b2Base_cellViewController(i2b2.SHRINE, pluginId);
    i2b2.SHRINE.plugin.enableRunQueryButton = enableRunQueryButton;
    i2b2.SHRINE.plugin.disableRunQueryButton = disableRunQueryButton;
    i2b2.SHRINE.plugin.errorDetail = errorDetail;
    
    function errorDetail(data) {

        var j$ = jQuery;
        j$('#pluginErrorDetail').remove();

        j$('body').append(j$(getDialogHTML(data)))

        var pluginErrorDetail = new YAHOO.widget.SimpleDialog("pluginErrorDetail", {
            width: "820px",
            fixedcenter: true,
            constraintoviewport: true,
            modal: true,
            zindex: 700,
            buttons: [{
                text: "Done",
                handler: function() {
                    this.cancel();
                },
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

        var wikiBaseUrl = (i2b2.hive.cfg.wikiBaseUrl || 'https://open.med.harvard.edu/wiki/display/SHRINE/');
        var codec = data.problemDigest && data.problemDigest.codec? data.problemDigest.codec : '';
        var html = '<div id="pluginErrorDetail" style="display:none;">' +
            '<div class="hd" style="background:#6677AA;">Query Error Detail</div>' +
            '<div class="bd">' +
            '<br />' +
            '<div style="border: 1px solid #C0C0C0; max-height: 450px;' +
            'background-color: #FFFFFF; overflow: scroll; word-wrap: break-word; padding: 10px 5px;"' +
            'class="StatusBoxText">' +
            '<div><b>Summary:</b></div>' +
            '<div>' + data.status + '</div><br/>' +
            '<div><b>Description:</b></div>' +
            '<div><p>' + data.statusMessage + '</p></div><br/>' +
            '<span id="pluginMoreDetail">' + 
                '<div><b>Codec:</b></div>' +
                '<div>' + (data.problemDigest && data.problemDigest.codec? data.problemDigest.codec : 'not available') + '</div>' +
                '<div><i>For information on troubleshooting and resolution, check' +
                ' <a href="' + wikiBaseUrl + codec +'" target="_blank">the SHRINE Error' +
                ' Codex</a>.</i>' +
                '</div><br/>' +
                '<div><b>Stamp:</b></div>' +
                '<div>' + (data.problemDigest && data.problemDigest.stampText? data.problemDigest.stampText : 'not available') + '</div><br/>' +
                '<div><b>Stack Trace Name:</b></div>' +
                '<div>' + (data.problemDigest && data.problemDigest.codec? data.problemDigest.codec : 'not available') + '</div><br/>' +
                '<div><b>Stack Trace Message:</b></div>' +
                '<div>' + (data.problemDigest && data.problemDigest.description? data.problemDigest.description : 'not available') + '</div><br/>' +
                '<div><b>Stack Trace Details:</b></div>' +
                '<div>' + (data.problemDigest && data.problemDigest.detailsString? data.problemDigest.detailsString.split(',').join(',<br/>') : 'not available') + '</div><br/>' +
            '</span>'
            '</div>' +
            '</div>' +
            '</div>';
            return html;
    }


    function disableRunQueryButton() {
        jQuery('#runBoxText').parent().bind('click', function(e) { e.preventDefault()});
    }

    function enableRunQueryButton() {
        jQuery('#runBoxText').parent().unbind('click');
    }
})();