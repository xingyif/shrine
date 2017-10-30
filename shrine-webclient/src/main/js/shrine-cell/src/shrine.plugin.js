/**
 * This Module is for all plugin related functionality that is exposed to the i2b2 framework.
 */
import I2B2Decorator from './common/i2b2.decorator';
import snippets from './common/shrine-snippets';
import dom from './common/shrine-dom';

export class ShrinePlugin extends I2B2Decorator {

    decorate(...rest) {
        super.decorate(...rest);
        const PLUGIN_ID = 'shrinePlugin';
        const CellViewController = this.global.i2b2Base_cellViewController;
        this.i2b2.SHRINE.plugin = new CellViewController(this.i2b2.SHRINE, PLUGIN_ID);
        this.i2b2.SHRINE.plugin.enableRunQueryButton = this.enableRunQueryButton.bind(this);
        this.i2b2.SHRINE.plugin.disableRunQueryButton = this.disableRunQueryButton.bind(this);
        this.i2b2.SHRINE.plugin.errorDetail = this.errorDetail.bind(this);
        const CustomEvent = this.YAHOO.util.CustomEvent;
        this.i2b2.events.networkIdReceived = new CustomEvent("networkIdReceived", this.i2b2);
        this.i2b2.events.afterQueryInit = new CustomEvent("afterQueryInit", this.i2b2);
        this.i2b2.events.queryResultAvailable = new CustomEvent("queryResultAvailable", this.i2b2);
        this.i2b2.events.queryResultUnavailable = new CustomEvent("queryResultUnvailable", this.i2b2);
        this.i2b2.events.exportQueryResult = new CustomEvent("exportQueryResult", this.i2b2);
        this.i2b2.events.clearQuery = new CustomEvent("clearQuery", this.i2b2);
        this.i2b2.events.queryResultAvailable.subscribe(this.queryResultAvailable.bind(this));
        this.i2b2.events.queryResultUnavailable.subscribe(this.queryResultUnavailable.bind(this));
    }

    enableRunQueryButton() {
        this.$('#runBoxText').parent().unbind('click');
    }

    disableRunQueryButton() {
        this.$('#runBoxText').parent().bind('click', e => e.preventDefault())
    }

    errorDetail(data) {
        this.$('#pluginErrorDetail').remove();
        this.$('body').append(this.$(snippets.dialogHTML(this.i2b2, data)));
        const SimpleDialog = this.YAHOO.widget.SimpleDialog;
        const pluginErrorDetail = new SimpleDialog("pluginErrorDetail", {
            width: "820px",
            fixedcenter: true,
            constraintoviewport: true,
            modal: true,
            zindex: 700,
            buttons: [{
                text: "Done",
                // -- function b/c yui handlers are used as bind and scopes, note 'this.cancel()' -- //
                handler: function () {
                    this.cancel();
                },
                isDefault: true
            }],
            validate: () => true
        });
        this.prototype$('pluginErrorDetail').show();
        pluginErrorDetail.render(document.body);
        pluginErrorDetail.center();
        pluginErrorDetail.show();
    }

    queryResultAvailable() {
        const csvExport = dom.shrineCSVExport();
        csvExport[0].onclick = e => {
            e.stopPropagation();
            this.i2b2.events.exportQueryResult.fire();
        }
        csvExport
          .css({ opacity: 1 })
        this.i2b2.SHRINE.plugin.enableRunQueryButton();
    }

    queryResultUnavailable() {
        const csvExport = dom.shrineCSVExport()
        csvExport[0].onclick = null;
        csvExport
          .css({ opacity: 0.25 });
    }
}
 // -- singleton -- //
export default new ShrinePlugin();