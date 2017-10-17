import I2B2Decorator from './common/i2b2.decorator';
import dom from './common/shrine-dom';
import snippets from './common/shrine-snippets';
import { startQueryMixin } from './mixins/CRC.ctrlr.QueryStatus';
import * as QTMixins from './mixins/CRC.ctrlr.QryTool';
import { contextMenuValidateMixin } from './mixins/CRC.view.History';

class ShrineBootstrapper extends I2B2Decorator {

  bootstrap() {
    this.polyfill();
    this.decorate();
    this.loadShrineWrapper();
  }

  decorate() {
    const CustomEvent = this.YAHOO.util.CustomEvent;
    this.i2b2.events.networkIdReceived = new CustomEvent("networkIdReceived", this.i2b2);
    this.i2b2.events.afterQueryInit = new CustomEvent("afterQueryInit", this.i2b2);
    this.i2b2.events.queryResultAvailable = new CustomEvent("queryResultAvailable", this.i2b2);
    this.i2b2.events.queryResultUnavailable = new CustomEvent("queryResultUnvailable", this.i2b2);
    this.i2b2.events.exportQueryResult = new CustomEvent("exportQueryResult", this.i2b2);
    this.i2b2.events.clearQuery = new CustomEvent("clearQuery", this.i2b2);

    const fireExportMsg = e => {
      e.stopPropagation();
      this.i2b2.events.exportQueryResult.fire();
    }

    this.i2b2.events.queryResultAvailable.subscribe(() => {
      dom.shrineCSVExport()
        .css({ opacity: 1 })
        .on('click', fireExportMsg)
      this.i2b2.SHRINE.plugin.enableRunQueryButton();
    });

    this.i2b2.events.queryResultUnavailable.subscribe(() => {
      dom.shrineCSVExport()
        .css({ opacity: 0.25 })
        .off('click', fireExportMsg);
    });

    this.i2b2.CRC.ctrlr.QT._queryRun = QTMixins.queryRunMixin(this)
    this.i2b2.CRC.view.status.showDisplay = () => { /*empty method*/ };
    this.i2b2.CRC.ctrlr.QT.doQueryClear = QTMixins.queryClearMixin(this);
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.startQuery = startQueryMixin(this);
    this.i2b2.CRC.view.history.ContextMenuValidate = contextMenuValidateMixin(this);
  }



  polyfill() {
    dom
      .hideI2B2Tabs()
      .hideI2B2Panels()
      .removeI2B2PrintIcon()
      .removeI2B2PrintQueryBox()
      .addExportIcon(snippets.shrineCSVExport)
      .addShrineTab(snippets.shrineTab)
      .addShrinePanel(snippets.shrinePanel)
      .shrineCSVExport(snippets.shrineCSVExport)
      .css({ opacity: 0.25 });
  }

  loadShrineWrapper() {
    return this.$(`#${this.i2b2.SHRINE.plugin.viewName}`).load(this.shrine.cfg.config.wrapperHtmlFile, (response, status, xhr) => {
      // -- callback implementation here --//
    });
  }
}

// -- singleton --//
export default new ShrineBootstrapper();