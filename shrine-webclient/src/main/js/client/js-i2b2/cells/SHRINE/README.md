 
# SHRINE References in I2B2 CODE:

### 	i2b2.events.networkIdReceived
- CRC_view_History.js: line 499
this code can be alternatively accesssed via: i2b2.CRC.view.history.ContextMenu

- CRC_ctrlr_QryStatus.js: line 337
this code can be accessed via: i2b2.CRC.ctrlr.QueryStatus.prototype.StartQuery, copy the code from private_startquery()

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



