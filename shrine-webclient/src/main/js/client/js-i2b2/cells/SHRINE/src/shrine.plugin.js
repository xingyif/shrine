import I2B2Decorator from './common/i2b2.decorator';
import snippets from './common/shrine-snippets';

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
    }
}

// -- closure functions have their scope altered by i2b2 -- //
const functions = {
    enableRunQueryButton: context => () => context.$('#runBoxText').parent().unbind('click'),
    disableRunQueryButton: context => () => context.$('#runBoxText').parent().bind('click', e => e.preventDefault()),
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