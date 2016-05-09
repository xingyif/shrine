/**
 * @projectDescription	View controller for the query status window (which is a GUI-only component of the CRC module).
 * @inherits 	i2b2.CRC.view
 * @namespace	i2b2.CRC.view.status
 * @author 		Nick Benik, Griffin Weber MD PhD
 * @version 	1.3
 * ----------------------------------------------------------------------------------------
 * updated 9-15-08: RC4 launch [Nick Benik]
 */
console.group('Load & Execute component file: CRC > view > Status');
console.time('execute time');


// create and save the screen objects
i2b2.CRC.view.status = new i2b2Base_cellViewController(i2b2.CRC, 'status');
i2b2.CRC.view.status.visible = false;

i2b2.CRC.view.status.show = function() {
	i2b2.CRC.view.status.visible = true;
	$('crcStatusBox').show();
}
i2b2.CRC.view.status.hide = function() {
	i2b2.CRC.view.status.visible = false;
	$('crcStatusBox').hide();
}

i2b2.CRC.view.status.hideDisplay = function() {
	$('infoQueryStatusText').hide();
}
i2b2.CRC.view.status.showDisplay = function() {
	var targs = $('infoQueryStatusText').parentNode.parentNode.select('DIV.tabBox.active');
	// remove all active tabs
	targs.each(function(el) { el.removeClassName('active'); });
	// set us as active
	$('infoQueryStatusText').parentNode.parentNode.select('DIV.tabBox.tabQueryStatus')[0].addClassName('active');
	$('infoQueryStatusText').show();
	//BG
	$('infoQueryStatusChart').hide(); 
	$('infoQueryStatusReport').hide();
	$('infoDownloadStatusData').hide();
	//BG
}

// ================================================================================================== //
//BG
i2b2.CRC.view.status.selectTab = function(tabCode) {
	// toggle between the Navigate and Find Terms tabs
	switch (tabCode) {
		case "graphs":
			this.currentTab = 'graphs';
			this.cellRoot.view['graphs'].showDisplay();
			this.cellRoot.view['status'].hideDisplay();
			this.cellRoot.view['queryReport'].hideDisplay();  
			this.cellRoot.view['downloadData'].hideDisplay();
			if(i2b2.CRC.ctrlr.currentQueryResults)
				i2b2.CRC.view.graphs.createGraphs("infoQueryStatusChart", i2b2.CRC.ctrlr.currentQueryResults.resultString, true);
			break;
		case "status":
			this.currentTab = 'status';
			this.cellRoot.view['status'].showDisplay();
			this.cellRoot.view['graphs'].hideDisplay();
			this.cellRoot.view['queryReport'].hideDisplay();  
			this.cellRoot.view['downloadData'].hideDisplay();
			break;
		case "queryReport":
			this.currentTab = 'queryReport';
			this.cellRoot.view['queryReport'].showDisplay();
			this.cellRoot.view['graphs'].hideDisplay();
			this.cellRoot.view['status'].hideDisplay();
			this.cellRoot.view['downloadData'].hideDisplay();
			break;
		case "downloadData":
			this.currentTab = 'downloadData';
			this.cellRoot.view['downloadData'].showDisplay();
			this.cellRoot.view['graphs'].hideDisplay();
			this.cellRoot.view['status'].hideDisplay();
			this.cellRoot.view['queryReport'].hideDisplay();
			if(i2b2.CRC.ctrlr.currentQueryResults){
				var resultsTable = jQuery("#infoDownloadStatusData").find("#resultsTable");
				if(resultsTable && resultsTable.length<=0)
					i2b2.CRC.view.downloadData.createCSV();
			}
			break;
	}
}
//BG
// ================================================================================================== //
i2b2.CRC.view.status.Resize = function(e) {
	var viewObj = i2b2.CRC.view.status;
	if (viewObj.visible) {
		//var ds = document.viewport.getDimensions();
 	    var w =  window.innerWidth || (window.document.documentElement.clientWidth || window.document.body.clientWidth);
 	    var h =  window.innerHeight || (window.document.documentElement.clientHeight || window.document.body.clientHeight);
		
		if (w < 840) {w = 840;}
		if (h < 517) {h = 517;}
		
		// resize our visual components
		var ve = $('crcStatusBox');
		ve.show();
		switch(viewObj.viewMode) {
			case "Patients":
				ve = ve.style;
				// keyoff splitter's position
				ve.left 	=  addToProperty($('main.splitter').style.left, 9, "px", "px" );
				ve.width 	= rightSideWidth - 51;
				//ve.left = w-550;
				//ve.width = 524;
				if (i2b2.WORK && i2b2.WORK.isLoaded) {
					$('infoQueryStatusText').style.height = '100px';
					if (YAHOO.env.ua.ie > 0) {  
						ve.top = h-135; //196+44;
					} else {
						ve.top = h-152; //196+44;
					}
				} else {
					$('infoQueryStatusText').style.height = '144px';
					ve.top = h-196;
				}
				break;
			default:
				ve.hide();
		}
	}
}
// ================================================================================================== //
// YAHOO.util.Event.addListener(window, "resize", i2b2.CRC.view.status.Resize, i2b2.CRC.view.status); // tdw9


//================================================================================================== //
i2b2.CRC.view.status.splitterDragged = function()
{
	//var viewPortDim = document.viewport.getDimensions();
 	var w =  window.innerWidth || (window.document.documentElement.clientWidth || window.document.body.clientWidth);
	var splitter = $( i2b2.hive.mySplitter.name );	
	var CRCStatus = $("crcStatusBox");
	CRCStatus.style.left	= (parseInt(splitter.offsetWidth) + parseInt(splitter.style.left) + 3) + "px";
	CRCStatus.style.width 	= Math.max(parseInt(w) - parseInt(splitter.style.left) - parseInt(splitter.offsetWidth) - 29, 0) + "px";
}

//================================================================================================== //
i2b2.CRC.view.status.ResizeHeight = function() 
{
	var viewObj = i2b2.CRC.view.status;
	if (viewObj.visible) {
		///var ds = document.viewport.getDimensions();
 	    var h =  window.innerHeight || (window.document.documentElement.clientHeight || window.document.body.clientHeight);
		if (h < 517) {h = 517;}
		// resize our visual components
		var ve = $('crcStatusBox');
		ve.show();
		switch(viewObj.viewMode) {
			case "Patients":
				ve = ve.style;
				if (i2b2.WORK && i2b2.WORK.isLoaded) 
				{
					if (i2b2.CRC.view.status.isZoomed) {
						$('infoQueryStatusText').style.height = h - 97;
						$('infoQueryStatusChart').style.height = h - 97;
						$('infoQueryStatusReport').style.height = h - 97;
						ve.top = 45;
						$('crcQueryToolBox').hide();
					} else {
						$('infoQueryStatusText').style.height = '100px';
						$('infoQueryStatusChart').style.height = '100px';//BG
						$('infoQueryStatusReport').style.height = '100px';//BG
						$('infoDownloadStatusData').style.height = '100px';//BG
						ve.top = h-198;
						$('crcQueryToolBox').show();
						
					}				
				} 
				else 
				{
					if (i2b2.CRC.view.status.isZoomed) {
						$('infoQueryStatusText').style.height = h - 97;
						$('infoQueryStatusChart').style.height = h - 97;
						$('infoQueryStatusReport').style.height = h - 97;
						$('infoDownloadStatusData').style.height = h - 97;
						ve.top = 45;
						$('crcQueryToolBox').hide();
					} else {
						$('infoQueryStatusText').style.height = '144px';
						$('infoQueryStatusChart').style.height = '144px';//BG
						$('infoQueryStatusReport').style.height = '144px';//BG
						$('infoDownloadStatusData').style.height = '144px';//BG
						ve.top = h-196;
						$('crcQueryToolBox').show();
					}
					
				}
				break;
			default:
				ve.hide();
		}
	}
}

// ================================================================================================== //

i2b2.CRC.view.status.ZoomView = function() {
	i2b2.hive.MasterView.toggleZoomWindow("status");
}

// ================================================================================================== //
i2b2.events.initView.subscribe((function(eventTypeName, newMode) {
// -------------------------------------------------------
	newMode = newMode[0];
	this.viewMode = newMode;
	this.visible = true;
	$('crcStatusBox').show();
	this.Resize();
// -------------------------------------------------------
}),'',i2b2.CRC.view.status);


//================================================================================================== //
i2b2.events.changedViewMode.subscribe((function(eventTypeName, newMode) {
// -------------------------------------------------------
	newMode = newMode[0];
	this.viewMode = newMode;
	switch(newMode) {
		case "Patients":
			// check if other windows are zoomed and blocking us
			var zw = i2b2.hive.MasterView.getZoomWindows();
			if (zw.member("QT")) {
				this.visible = false;
			} else {
				this.visible = true;
			}
			break;
		default:
			this.visible = false;
			break;
	}
	if (this.visible) {
		$('crcStatusBox').show();
		i2b2.CRC.view.status.splitterDragged();
		//this.Resize();	// tdw9
	} else {
		$('crcStatusBox').hide();		
	}
// -------------------------------------------------------
}),'',i2b2.CRC.view.status);


// ================================================================================================== //
i2b2.events.changedZoomWindows.subscribe((function(eventTypeName, zoomMsg) {
	newMode = zoomMsg[0];
	if (!newMode.action) { return; }
	if (newMode.action == "ADD") {
		switch (newMode.window) {
			case "QT":
				this.visible = false;
				this.isZoomed = false;
				i2b2.CRC.view.status.hide();
				break;
			case "status":
				this.isZoomed = true;
				this.visible = true;
		}
	} else {
		switch (newMode.window) {
			case "QT":
				this.isZoomed = false;
				this.visible = true;
				i2b2.CRC.view.status.show();
				break;
			case "status":
				this.isZoomed = false;
				this.visible = true;
		}
	}
	this.ResizeHeight();
	//this.Resize();		// tdw9
}),'',i2b2.CRC.view.status);



console.timeEnd('execute time');
console.groupEnd();