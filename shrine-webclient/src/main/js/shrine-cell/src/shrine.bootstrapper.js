/**
 * This Module is altering/bootstrapping the i2b2 UI to accomodate Shrine.
 */
import I2B2Decorator from './common/i2b2.decorator';
import dom from './common/shrine-dom';
import snippets from './common/shrine-snippets';
import { startQueryMixin, isQueryRunningMixin, refreshStatusMixin } from './mixins/CRC.ctrlr.QueryStatus';
import * as QTMixins from './mixins/CRC.ctrlr.QryTool';
import ResizeHeightMixin from './mixins/CRC.view.Status';
import getChildRecordsMixin from './mixins/CRC.sdx.QM';
import { MixinNoSynonymTermInfo } from './mixins/i2b2.messages';

class ShrineBootstrapper extends I2B2Decorator{

  decorate(...rest) {
    super.decorate(...rest);
    this.polyfill();
    this.mixins();
    this.loadShrineWrapper();
  }

  mixins() {
    this.i2b2.CRC.ctrlr.QT._queryRun = QTMixins.queryRunMixin(this)
    this.i2b2.CRC.view.status.showDisplay = () => { /*empty method to not break referenes from i2b2*/ };
    this.i2b2.CRC.ctrlr.QT.doQueryClear = QTMixins.queryClearMixin(this);
    this.i2b2.h.LoadingMask.show = () => { /* empty emthod to not break references from i2b2*/ };
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.startQuery = startQueryMixin(this);
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.isQueryRunning = isQueryRunningMixin();
    this.i2b2.CRC.ctrlr.QueryStatus.prototype.refreshStatus = refreshStatusMixin();
    this.i2b2.CRC.view.status.ResizeHeight  = ResizeHeightMixin(this);
    this.i2b2.sdx.TypeControllers.QM.getChildRecords = getChildRecordsMixin(this);
    MixinNoSynonymTermInfo(this)(false);
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

    this.i2b2.hive.mySplitter.onMouseDown  = this.onMouseDown.bind(this);
    this.i2b2.hive.mySplitter.onMouseUp = this.onMouseUp.bind(this);
    const roles = this.i2b2.PM.model.userRoles.filter(r => r !== 'MANAGER');
    this.i2b2.PM.model.userRoles = roles;
  }

  onMouseDown(e) {
    this.$('#shrine-iframe').css('display', 'none')
  }

  onMouseUp(e) {
    this.$('#shrine-iframe').css('display', 'inline')
  }

  loadShrineWrapper() {
    return this.$(`#${this.i2b2.SHRINE.plugin.viewName}`).load(this.i2b2.SHRINE.cfg.config.wrapperHtmlFile, (response, status, xhr) => {
      // -- callback implementation here --//
    });
  }
}

// -- singleton --//
export default new ShrineBootstrapper();