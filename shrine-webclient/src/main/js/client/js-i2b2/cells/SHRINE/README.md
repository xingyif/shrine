 
# SHRINE References in I2B2 CODE:

### 	i2b2.events.networkIdReceived
- CRC_view_History.js: line 499
this code can be alternatively accesssed via: i2b2.CRC.view.history.ContextMenu

- CRC_ctrlr_QryStatus.js: line 337
this code can be accessed via: i2b2.CRC.ctrlr.QueryStatus.prototype.startQuery, copy the code from private_startquery()
```javascript
	function private_startQuery() {
		var self = i2b2.CRC.ctrlr.currentQueryStatus;
		var resultString = ""; //BG

		if (private_singleton_isRunning) {
			return false;
		}

		private_singleton_isRunning = true;
		//BG
		var downloadDataTab = $('infoDownloadStatusData');
		if (downloadDataTab)
			downloadDataTab.innerHTML = "";
		i2b2.CRC.ctrlr.currentQueryResults = new i2b2.CRC.ctrlr.QueryResults(resultString);
		//BG
		self.dispDIV.innerHTML = '<b>Processing Query: "' + this.name + '"</b>';
		self.QM.name = this.name;
		self.QRS = [];
		self.QI = {};

		// callback processor to run the query from definition
		this.callbackQueryDef = new i2b2_scopedCallback();
		this.callbackQueryDef.scope = this;

		this.callbackQueryDef.callback = function (results) {
			if(!i2b2.CRC.ctrlr.currentQueryStatus) return;
			var networkId = results.refXML.getElementsByTagName('query_master_id')[0].firstChild.nodeValue;
			i2b2.events.networkIdReceived.fire({networkId: networkId});
			i2b2.CRC.ctrlr.history.Refresh();
			clearQuery();
		}

		function clearQuery() {
			clearInterval(private_refreshInterrupt);
			private_refreshInterrupt = false;
			private_singleton_isRunning = false;
			$('runBoxText').innerHTML = "Run Query";
			i2b2.CRC.ctrlr.currentQueryStatus = false;
			jQuery('#dialogQryRunResultType input[type="checkbox"]')
				.each(function(a, b) { b.checked = b.disabled });
		}

		// switch to status tab
		i2b2.CRC.view.status.showDisplay();

		// timer and display refresh stuff
		private_startTime = new Date();
		private_refreshInterrupt = setInterval("i2b2.CRC.ctrlr.currentQueryStatus.refreshStatus()", 100);

		// AJAX call
		i2b2.CRC.ajax.runQueryInstance_fromQueryDefinition("CRC:QueryTool", this.params, this.callbackQueryDef);
  }
  ```

### i2b2.SHRINE.plugin 
- CRC_ctrlr_QryStatus.js: line 151, line 303
this code can be accessed via i2b2.CRC.ctrlr.QueryStatus.prototype.refreshStatus (you can copy existing code and override it.)

- CRC_ctrlr_QryTool.js: line 64
this code can be accessed by making a copy of i2b2.CRC.ctrlr.QT.doQueryClear and calling it after or before referencing the plugin.

# Overridden I2B2 Methods
- i2b2.CRC.ctrlr.QT.doQueryClear
- i2b2.CRC.ctrlr.QT._queryRun;

# shrinePlugin refrences:
- CRC_view_Status.js: line 158, line 166, line 179, line 187
this section can be referenced by copying the code for i2b2.CRC.view.status.ResizeHeight to include the shrinePlugin references.

- vwStatus.css line 53
these styles can be inlined.

# shrine-iframe references:
- hive.ui.js: line 139, line 143,  splitter can be referenced globally by i2b2.hive.mySplitter.onMouseUp and i2b2.hive.mySplitter.onMouseDown



