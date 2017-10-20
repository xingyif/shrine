/**
 * This Module is altering/bootstrapping the i2b2 UI to accomodate Shrine.
 */
import I2B2Decorator from './common/i2b2.decorator';
import dom from './common/shrine-dom';
import snippets from './common/shrine-snippets';
import { startQueryMixin, isQueryRunningMixin, refreshStatusMixin } from './mixins/CRC.ctrlr.QueryStatus';
import * as QTMixins from './mixins/CRC.ctrlr.QryTool';
import { contextMenuValidateMixin } from './mixins/CRC.view.History';

class ShrineBootstrapper extends I2B2Decorator {

  bootstrap() {
    this.polyfill();
    this.decorate();
    this.loadShrineWrapper();
  }

  decorate() {
    this.i2b2.CRC.ctrlr.QT._queryRun = QTMixins.queryRunMixin(this)
    this.i2b2.CRC.view.status.showDisplay = () => { /*empty method to not break referenes from i2b2*/ };
    this.i2b2.CRC.ctrlr.QT.doQueryClear = QTMixins.queryClearMixin(this);
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.startQuery = startQueryMixin(this);
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.isQueryRunning = isQueryRunningMixin();
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.refreshStatus = refreshStatusMixin();
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