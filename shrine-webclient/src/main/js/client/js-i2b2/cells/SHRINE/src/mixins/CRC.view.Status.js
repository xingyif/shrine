const ResizeHeightMixin = context => () => {
  var viewObj = context.i2b2.CRC.view.status;
	if (viewObj.visible) {
		///var ds = document.viewport.getDimensions();
 	  var h =  window.innerHeight || (window.document.documentElement.clientHeight || window.document.body.clientHeight);
		if (h < 517) {h = 517;}
		// resize our visual components
		var ve = context.prototype$('crcStatusBox');
		ve.show();
		switch(viewObj.viewMode) {
			case "Patients":
				ve = ve.style;
				if (context.i2b2.WORK && context.i2b2.WORK.isLoaded) 
				{
					if (context.i2b2.CRC.view.status.isZoomed) {
						context.prototype$('infoQueryStatusText').style.height = h - 97;
						context.prototype$('infoQueryStatusChart').style.height = h - 97;
						context.prototype$('infoQueryStatusReport').style.height = h - 97;
						context.prototype$('shrinePlugin').style.height = h - 97;
						ve.top = 45;
						context.prototype$('crcQueryToolBox').hide();
					} else {
						context.prototype$('infoQueryStatusText').style.height = '100px';
						context.prototype$('infoQueryStatusChart').style.height = '100px';//BG
						context.prototype$('infoQueryStatusReport').style.height = '100px';//BG
						context.prototype$('infoDownloadStatusData').style.height = '100px';//BG
						context.prototype$('shrinePlugin').style.height = '100px';
						ve.top = h-198;
						context.prototype$('crcQueryToolBox').show();
						
					}				
				} 
				else 
				{
					if (context.i2b2.CRC.view.status.isZoomed) {
						context.prototype$('infoQueryStatusText').style.height = h - 97;
						context.prototype$('infoQueryStatusChart').style.height = h - 97;
						context.prototype$('infoQueryStatusReport').style.height = h - 97;
						context.prototype$('infoDownloadStatusData').style.height = h - 97;
						context.prototype$('shrinePlugin').style.height = h - 97;
						ve.top = 45;
						context.prototype$('crcQueryToolBox').hide();
					} else {
						context.prototype$('infoQueryStatusText').style.height = '144px';
						context.prototype$('infoQueryStatusChart').style.height = '144px';//BG
						context.prototype$('infoQueryStatusReport').style.height = '144px';//BG
						context.prototype$('infoDownloadStatusData').style.height = '144px';//BG
						context.prototype$('shrinePlugin').style.height = '144px';
						ve.top = h-196;
						context.prototype$('crcQueryToolBox').show();
					}
					
				}
				break;
			default:
				ve.hide();
		}
	}
}
export default ResizeHeightMixin;