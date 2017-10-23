/**
 * This Module is for all plugin related functionality that is exposed to the i2b2 framework.
 */
import I2B2Decorator from './common/i2b2.decorator';
import snippets from './common/shrine-snippets';
import dom from './common/shrine-dom';

export class ShrinePlugin extends I2B2Decorator {
    constructor() {
        super();
    }

    decorate() {
        const PLUGIN_ID = 'shrinePlugin';
        const CellViewController = this.global.i2b2Base_cellViewController;
        this.i2b2.SHRINE.plugin = new CellViewController(this.i2b2.SHRINE, PLUGIN_ID);
        this.i2b2.SHRINE.plugin.enableRunQueryButton = functions.enableRunQueryButton(this);
        this.i2b2.SHRINE.plugin.disableRunQueryButton = functions.disableRunQueryButton(this);
        this.i2b2.SHRINE.plugin.errorDetail = functions.errorDetail(this, this.YAHOO.widget.SimpleDialog);
        const CustomEvent = this.YAHOO.util.CustomEvent;
        this.i2b2.events.networkIdReceived = new CustomEvent("networkIdReceived", this.i2b2);
        this.i2b2.events.afterQueryInit = new CustomEvent("afterQueryInit", this.i2b2);
        this.i2b2.events.queryResultAvailable = new CustomEvent("queryResultAvailable", this.i2b2);
        this.i2b2.events.queryResultUnavailable = new CustomEvent("queryResultUnvailable", this.i2b2);
        this.i2b2.events.exportQueryResult = new CustomEvent("exportQueryResult", this.i2b2);
        this.i2b2.events.clearQuery = new CustomEvent("clearQuery", this.i2b2);
        this.i2b2.events.queryResultAvailable.subscribe(functions.queryResultAvailable(this));
        this.i2b2.events.queryResultUnavailable.subscribe(() => {
          const csvExport = dom.shrineCSVExport()
          csvExport[0].onclick = null;
          csvExport
            .css({ opacity: 0.25 });
        });
    }
}

// -- closure functions have their scope altered by i2b2 -- //
const functions = {
    enableRunQueryButton: context => () => context.$('#runBoxText').parent().unbind('click'),
    disableRunQueryButton: context => () => context.$('#runBoxText').parent().bind('click', e => e.preventDefault()),
    queryResultAvailable: context => () => {
        const csvExport = dom.shrineCSVExport();
        csvExport[0].onclick = e => {
            e.stopPropagation();
            context.i2b2.events.exportQueryResult.fire();
        }
        csvExport
          .css({ opacity: 1 })
        context.i2b2.SHRINE.plugin.enableRunQueryButton();
    },
    errorDetail: context => data => {
        context.$('#pluginErrorDetail').remove();
        context.$('body').append(context.$(snippets.dialogHTML(context.i2b2, data)));
        const SimpleDialog = context.YAHOO.widget.SimpleDialog;
        const pluginErrorDetail = new SimpleDialog("pluginErrorDetail", {
            width: "820px",
            fixedcenter: true,
            constraintoviewport: true,
            modal: true,
            zindex: 700,
            buttons: [{
                text: "Done",
                // -- function b/c yui handlers are used as functions and scopes, note 'this.cancel()' -- //
                handler: function () {
                    this.cancel();
                },
                isDefault: true
            }],
            validate: () => true
        });
        context.prototype$('pluginErrorDetail').show();
        pluginErrorDetail.render(document.body);
        pluginErrorDetail.center();
        pluginErrorDetail.show();
    }
}

export default new ShrinePlugin();