export const startQueryMixin = (context) =>

  function (queryName, ajaxParams) {
    const self = context.i2b2.CRC.ctrlr.currentQueryStatus;
    if(self.isQueryRunning()) return;
    self.isQueryRunning(true);
    self.name = queryName; // for consistency with i2b2.
    self.params = ajaxParams;
    context.i2b2.CRC.view.status.showDisplay();
    context.i2b2.CRC.ctrlr.currentQueryResults = new context.i2b2.CRC.ctrlr.QueryResults(queryName);

    // callback processor to run the query from definition
    this.callbackQueryDef = new context.global.i2b2_scopedCallback();
    this.callbackQueryDef.scope = this;
    this.callbackQueryDef.callback = results => {
      if (!context.i2b2.CRC.ctrlr.currentQueryStatus) return;
      var networkId = results.refXML.getElementsByTagName('query_master_id')[0].firstChild.nodeValue;
      context.i2b2.events.networkIdReceived.fire({ networkId: networkId });
      clearQuery();
      context.i2b2.CRC.view.history.doRefreshAll();
    }

    const clearQuery = () => {
      self.isQueryRunning(false);
      context.prototype$('runBoxText').innerHTML = "Run Query";
      self.currentQueryStatus = false;
      context.$('#dialogQryRunResultType input[type="checkbox"]')
        .each(function (a, b) { b.checked = b.disabled });
      context.i2b2.SHRINE.plugin.enableRunQueryButton();
    }

    const params = context.i2b2.CRC.ctrlr.currentQueryStatus.params;
    context.i2b2.CRC.ajax.runQueryInstance_fromQueryDefinition("CRC:QueryTool", ajaxParams, this.callbackQueryDef);
  }

export const isQueryRunningMixin = (running = false) =>
  (state) => {
    if(state !== undefined) running = state;
    return running;
  }

export const refreshStatusMixin = () => () => {/* empty method for compatibility with i2b2 */}

