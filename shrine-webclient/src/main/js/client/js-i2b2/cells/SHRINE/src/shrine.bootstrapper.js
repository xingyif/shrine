import I2B2Decorator from './i2b2.decorator';
import dom from './shrine-dom';
import snippets from './shrine-snippets';

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

    this.i2b2.events.queryResultUnavailable.subscribe(() =>{
      dom.shrineCSVExport()
        .css({ opacity: 0.25 })
        .off('click', fireExportMsg);
    });

    // -- i2b2 overrides -- //
    const queryRun = this.i2b2.CRC.ctrlr.QT._queryRun;
    this.i2b2.CRC.ctrlr.QT._queryRun = (name, options) => {
      this.shrine.plugin.disableRunQueryButton();
      this.i2b2.events.afterQueryInit.fire({ name: name, data: options });
      return queryRun.apply(this.i2b2.CRC.ctrlr.QT, [name, options]);
    }

    this.i2b2.CRC.view.status.showDisplay = () => 
      console.log('i2b2.CRC.view.status.showDisplay overridden by SHRINE');

    const doQueryClear = this.i2b2.CRC.ctrlr.QT.doQueryClear;
    this.i2b2.CRC.ctrlr.QT.doQueryClear = clearStatus => {
      doQueryClear.apply(this.i2b2.CRC.ctrlr.QT, []);
      if (clearStatus === true) this.i2b2.events.clearQuery.fire();
    }    
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