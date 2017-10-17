const startQuery = (context) => 
  function (queryName, ajaxParams) {
    const self = context.i2b2.CRC.ctrlr.currentQueryStatus;
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
    }

    const clearQuery = () => {
      context.prototype$('runBoxText').innerHTML = "Run Query";
      self.currentQueryStatus = false;
      context.$('#dialogQryRunResultType input[type="checkbox"]')
        .each(function (a, b) { b.checked = b.disabled });
    }

    const params = context.i2b2.CRC.ctrlr.currentQueryStatus.params;
    context.i2b2.CRC.ajax.runQueryInstance_fromQueryDefinition("CRC:QueryTool", ajaxParams, this.callbackQueryDef);
  }

  export default startQuery;
