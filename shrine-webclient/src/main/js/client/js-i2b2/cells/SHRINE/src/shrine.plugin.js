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
        this.i2b2.SHRINE.plugin.enableRunQueryButton = this.enableRunQueryButton(this);
        this.i2b2.SHRINE.plugin.disableRunQueryButton = this.disableRunQueryButton(this);
        this.i2b2.SHRINE.plugin.errorDetail = this.errorDetail(this, this.YAHOO.widget.SimpleDialog);
    }

    errorDetail(me, SimpleDialog) {
        return data => {
            me.$('#pluginErrorDetail').remove();
            me.$('body').append(me.$(snippets.dialogHTML(me.i2b2, data)));
            const pluginErrorDetail = new SimpleDialog("pluginErrorDetail", {
                width: "820px",
                fixedcenter: true,
                constraintoviewport: true,
                modal: true,
                zindex: 700,
                buttons: [{
                    text: "Done",
                    handler: () => {
                        this.cancel();
                    },
                    isDefault: true
                }],
                validate: () => true
            });
            me.prototype$('pluginErrorDetail').show();
            pluginErrorDetail.render(document.body);
            pluginErrorDetail.center();
            pluginErrorDetail.show();
        }
    }
    enableRunQueryButton(me) {
        return () => me.$('#runBoxText').parent().unbind('click');
    }
    disableRunQueryButton(me) {
        return () => me.$('#runBoxText').parent().bind('click', e => e.preventDefault());
    }
}

export default new ShrinePlugin();