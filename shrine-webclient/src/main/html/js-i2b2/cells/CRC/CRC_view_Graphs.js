/** -----------------------------------------------------------------------------------------------------------------------
 * @projectDescription	View controller for the query graph window (which is a GUI-only component of the CRC module).
 * @inherits 	i2b2.CRC.view
 * @namespace	i2b2.CRC.view.graphs
 * @author 		Shawn Murphy MD PhD, Hannah Murphy
 * @version 	1.7
 * @description This set of functions uses D3 and its derivative C3 to graph the text that results in the query-status-window
 *              of the i2b2 web client.  
 *              The main function is "createGraphs".
 *              Because it makes extensive use of Vector Graphic in the D3 library it will only work in Microsoft Internet 
 *              Explorer 9 and above.  It assumes the STATUS window is a specific height which is 146px.  In theory the 
 *              Width can vary, but right not it is set to 535px in many places.
 *              It draws the graphs in a div (which should be the dimensions above), using a string which is essentially 
 *              screen-scraped from the text what is placed in the query_status box of the web client.  To distinguish the
 *              normal i2b2 vs. SHRINE text, a boolean flag is used.  A regular i2b2 result (bIsMultiSite = false)
 *              or a SHRINE result (bIsMultiSite = true).
 *                   Internally, everything works off an array the is produced from the test that is a six element array of 
 *              ****  0 "query name", 1 "title", 2 "site", 3 "element name", 4 quantity, 5 "sentence"  ****
 *              for example, one element would be:
 *              **** ["Circulatory sys@20:21:19", "Age patient breakdown", "MGH" "0-9 years old", 0, "0-9 years old: 0"] ****
 *              It also uses some jQuery, but only for the scroll bar function with *** jQuery Stuff ***
 *              in the comments.
 *              There are four web client javascript files in the CRC folder that have references to functions in this 
 *              javascript file, and the default.htm folder in the main web client folder, they are:
 *              CRC_view_Status, CRC_ctlr_QryStatus, CRC_ctlr_QryTools, and cell_config_data
 ** -----------------------------------------------------------------------------------------------------------------------*/
console.group('Load & Execute component file: CRC > view > Graphs');
console.time('execute time');

//i2b2.PM.model.isObfuscated =  true; // for testing
	
// Constants
var msSpecialBreakdownSite = "";  // this constant designates a site from which the breakdown arrays will always be obtained
var msStringDefineingNumberOfPatients = "number of patients";  // this constant is what appears in the breakdown text 
//                            which is the number of patients and is lower cased and trimmed on both sides for comparison

// create and save the screen objects and more constants

i2b2.CRC.view.graphs = new i2b2Base_cellViewController(i2b2.CRC, 'graphs');
i2b2.CRC.view.graphs.visible = false;
i2b2.CRC.view.graphs.iObfuscatedFloorNumber = 3;  // this is the amount reported that the numbers are obfuscated by
i2b2.CRC.view.graphs.sObfuscatedText = "<3";  // this is the text that is replaced for a small number in obfuscated mode
//                            so that it can be cleaned up before the next display
i2b2.CRC.view.graphs.sObfuscatedEnding = "&plusmn;3";  //this is the text that is added to all numbers in obfuscated mode
i2b2.CRC.view.graphs.sObfuscatedSHRINEText = "10 patients or fewer";  // this is the text that is replaced for a small number in obfuscated mode //todo fix with SHRINE-1716
//                            so that it can be cleaned up before the next display
i2b2.CRC.view.graphs.sObfuscatedSHRINEEnding = "+-10 patients";  //this is the text that is added to all numbers in obfuscated mode //todo fix with SHRINE-1716
//i2b2.CRC.view.graphs.bIsSHRINE = false;  // this changes the way the graphs are made if the file is being run in SHRINE mode
//                            NOTE THAT THIS IS DEMO ONLY IN THIS VERSION - IT DOES NOT REALLY WORK
i2b2.CRC.view.graphs.asTitleOfShrineGroup = [];

// These functions manage the graph divs, but DECISIONS are made in the CRC_view_Status code

i2b2.CRC.view.graphs.show = function() {
	i2b2.CRC.view.graphs.visible = true;
	$('crcGraphsBox').show();
}
i2b2.CRC.view.graphs.hide = function() {
	i2b2.CRC.view.graphs.visible = false;
	$('crcGraphsBox').hide();
}

i2b2.CRC.view.graphs.showDisplay = function() {
	var targs = $('infoQueryStatusChart').parentNode.parentNode.select('DIV.tabBox.active');
	// remove all active tabs
	targs.each(function(el) { el.removeClassName('active'); });
	// set us as active
	$('infoQueryStatusChart').parentNode.parentNode.select('DIV.tabBox.tabQueryGraphs')[0].addClassName('active');
	$('infoQueryStatusChart').show();
}

i2b2.CRC.view.graphs.hideDisplay = function() {
	$('infoQueryStatusChart').hide();
}

// ================================================================================================== //


/*********************************************************************************
   FUNCTION createGraphs
   Takes a Div, the text from the query status view, and a multisite flag and populates the Div
**********************************************************************************/
i2b2.CRC.view.graphs.createGraphs = function(sDivName, sInputString, bIsMultiSite) {
	try {
		if (sDivName === undefined || sDivName === null || sDivName === "") throw ("ERROR 201 - sDivName in function createGraphs is null");
		//i2b2.CRC.view.graphs.sNameOfPreviousDiv = sDivName;
		i2b2.CRC.view.graphs.clearGraphs(sDivName);
		if (!i2b2.CRC.view.graphs.bisGTIE8) {
			i2b2.CRC.view.graphs.aDivForIE8(sDivName);
			return;
		}
		// if (bIsMultiSite) sInputString = i2b2.CRC.view.graphs.returnTestString(true);  //For testing
		
		if (sInputString === undefined || sInputString === null || sInputString === "") throw ("ERROR 202 - sInputString in function createGraphs is null");
		var asBreakdownArray = [[]];
		var iBreakdown = 0;

		// make the input array  
		var asInputArray = i2b2.CRC.view.graphs.parseInputIntoArray(sInputString, bIsMultiSite);
		// Pull out unique breakdown types
		var asBreakdownTypes = [];
		var iBreakdown = 0;
		for (var i = 0; i < asInputArray.length; i++) {
				asBreakdownTypes[iBreakdown] = asInputArray[i][1];
				iBreakdown++;
		}
		var asUniqueBreakdownTypes = [];
		for (var i=0; i < asBreakdownTypes.length; i++) {
			if (asUniqueBreakdownTypes.indexOf(asBreakdownTypes[i]) === -1 && asBreakdownTypes[i] !== ''&& (!(asBreakdownTypes[i].toLowerCase().indexOf('error')>=0)))
				asUniqueBreakdownTypes.push(asBreakdownTypes[i]);
		}
		if (asUniqueBreakdownTypes.length === 0) throw ("ERROR 203 in createGraphs, there are no breakdown types in *unique* array");
		// rearrange unique array so that patient number is on the top
		for (var i = 0; i < asUniqueBreakdownTypes.length; i++) {
			if (asUniqueBreakdownTypes[i].toLowerCase().trim() == msStringDefineingNumberOfPatients.toLowerCase().trim()) {
				var sTempVariable = asUniqueBreakdownTypes[0];
				asUniqueBreakdownTypes[0] = asUniqueBreakdownTypes[i];
				asUniqueBreakdownTypes[i] = sTempVariable;
				break;
			}
		}

		//Make Divs in the original div for the charts
		oParentDiv = document.getElementById(sDivName);
		for (var i=0; i<asUniqueBreakdownTypes.length; i++){
			sChartDivName = "chart"+i;
			var child = document.createElement("div");
			child.setAttribute("id",sChartDivName);
			child.setAttribute("class","chartDiv");
			var chartTitleId = "chartTitle" + i;
			var childTitle = document.createElement("div");
			childTitle.setAttribute("id",chartTitleId);
			childTitle.setAttribute("class","chartTitleDiv");
			// child.setAttribute("width","auto");
			oParentDiv.appendChild(childTitle);
			oParentDiv.appendChild(child);
			
		}
		// populate each Div with a graph
		// populate the Div that has the number of patients first to make it the top one
		if (!bIsMultiSite) {
			var iIncrement = 0;
			if (asUniqueBreakdownTypes[0].toLowerCase().trim() == msStringDefineingNumberOfPatients.toLowerCase().trim()) {
				graph_singlesite_patient_number("chart0", asUniqueBreakdownTypes[0], asInputArray);
				iIncrement = 1;
			}
			//graph_singlesite_patient_number("chart0", asUniqueBreakdownTypes[0], asInputArray);
			for (var i=0+iIncrement; i<asUniqueBreakdownTypes.length; i++){
				graph_singlesite_patient_breakdown("chart"+i, asUniqueBreakdownTypes[i], asInputArray);
			}
		}
		else {
			var _2DResultsArray = i2b2.CRC.view.downloadData.getFormattedResults(asInputArray);
			if(_2DResultsArray)
			{
				var siteReturningResults = $H();
				var sitesReturningError = $H();
				var allSites = _2DResultsArray[0];
				if(allSites && allSites.length>0)
				{
					for(var x = 1; x < allSites.length ; x++)
					{
						var siteName = allSites[x];
						siteReturningResults.set(siteName,x);  //SiteName,Index pairs
						sitesReturningError.set(siteName,true);
					}
				}
				
				for(var x = 2; x < _2DResultsArray.length ; x++)  //Skipping the site name and patient count rows
				{
					var thisSiteData = _2DResultsArray[x];
					for(var y = 1 ; y < thisSiteData.length; y++)  //Skip the category name column
					{
						if(thisSiteData[y] != " ")
						{
							var siteName = null;
							siteReturningResults.each(function(item){
								if(item.value==y)
								{
									siteName = item.key;
								}
							});
							if(siteName){
								sitesReturningError.unset(siteName);
								
							}
						}
							
					}
				}
				
				sitesReturningError.each(function(item){
					if(siteReturningResults.get(item.key))
						siteReturningResults.unset(item.key);
				});
			}
			
			var titleDiv = jQuery("#" + sDivName + ' #chartTitle0');
			if(titleDiv){
				titleDiv.html("Patient Count     ");
				var zoomLink = jQuery("<a href = \"javascript:void(0);\" id=\"zoomLink\" onclick=\"javascript:i2b2.CRC.view.graphs.zoomGraph(event);\">Zoom Graph</a>");
				zoomLink[0].dataArray = asInputArray;
				zoomLink[0].graphTitle = "Patient Count";
				titleDiv.append(zoomLink);
			}
			graph_multiplesite_patient_number("#" + sDivName + " #chart0", asUniqueBreakdownTypes[0], asInputArray);

			for (var i=1; i<asUniqueBreakdownTypes.length; i++){
				var chartDivId = "chart"+i;
				var title = asUniqueBreakdownTypes[i];
				if(title.indexOf('ERROR')>=0) continue;
				if(title.toLowerCase().indexOf('vital')>=0)
					title = title + ' Status';
				
				var chartTitle = "Patient " + title + " Count Breakdown     ";
				var titleDiv = jQuery("#" + sDivName + ' #chartTitle' + i);
				var dataArray = i2b2.CRC.view.graphs.getGraphDataArray(_2DResultsArray,asUniqueBreakdownTypes[i],siteReturningResults);
				if(titleDiv){
					titleDiv.html(chartTitle);
					var zoomLink = jQuery("<a href = \"javascript:void(0);\" id=\"zoomLink\" onclick=\"javascript:i2b2.CRC.view.graphs.zoomGraph(event);\">Zoom Graph</a>");
					zoomLink[0].dataArray = dataArray;
					zoomLink[0].graphTitle = chartTitle;
					zoomLink[0].groups = siteReturningResults.keys();
					titleDiv.append(zoomLink);
				}
				graph_multiplesite_patient_breakdown("#" + sDivName + " #" + chartDivId, siteReturningResults.keys(), dataArray);
			}	
			
		 }
	}
	catch(err) {
		console.error(err);
	}
} // END of function createGraphs

i2b2.CRC.view.graphs.zoomGraph = function(event) 
{
	try{
		var wWidth = jQuery(window).width();
		var dWidth = wWidth * 0.95;
		var wHeight = jQuery(window).height();
		if( navigator.userAgent.toLowerCase().indexOf('firefox') > -1 ){
			wHeight = window.innerHeight;
		}
		var dHeight = wHeight * 0.95;
		
		var thisLinkElem = jQuery(event.target);
		var data = [];
		var chartTitle = "";
		if(thisLinkElem && thisLinkElem.length>0)
		{
			data = thisLinkElem[0].dataArray;
			chartTitle = thisLinkElem[0].graphTitle;
			
			if (i2b2.CRC.view.dialogZoomedGraph) 
				i2b2.CRC.view.dialogZoomedGraph = null;
				
			i2b2.CRC.view.dialogZoomedGraph = new YAHOO.widget.SimpleDialog("dialogZoomedGraph", {
					width: dWidth,
					height: dHeight,
					fixedcenter: true,
					constraintoviewport: true,
					modal: true,
					zindex: 700,
				});
			$('dialogZoomedGraph').show();
			i2b2.CRC.view.dialogZoomedGraph.render(document.body);
			// display the dialoge
			i2b2.CRC.view.dialogZoomedGraph.center();
			i2b2.CRC.view.dialogZoomedGraph.show();
			
			jQuery("#zoomedGraphTitle").addClass('chartTitleDiv').html(chartTitle);
			
			if(chartTitle!=""){
				if(chartTitle.indexOf("Patient Count")>=0)
					graph_multiplesite_patient_number("#zoomedGraphBody" , chartTitle, data, wHeight * 0.80);
				else
					graph_multiplesite_patient_breakdown("#zoomedGraphBody",thisLinkElem[0].groups , data, wHeight * 0.80);
			}
			
			i2b2.CRC.view.dialogZoomedGraph.hideEvent.subscribe(function(o) {
				jQuery("#zoomedGraphTitle").html("");
				jQuery("#zoomedGraphBody").html("");
			});
			
		}
		else
			alert("Graph can't be zoomed!");
	}
	catch(e)
	{
		console.error(e);
		alert("Graph can't be zoomed!");
	}
}

i2b2.CRC.view.graphs.getGraphDataArray = function(_2DResultsArray,breakdownType,siteHash) {
	var maxRowsNum = siteHash.size();
	if(maxRowsNum>0)
	{
		var breakdownHash = $H();
		
		for(x=2;x<_2DResultsArray.length;x++)  //Skip the first row which consists of only site names and 2nd row that has total patient count
		{
			var thisRow = _2DResultsArray[x];
			var category = thisRow[0];
			if(category == " ") continue;
			if(category.toLowerCase().indexOf(breakdownType.toLowerCase())>=0)
			{
				breakdownHash.set(category,x);
			}
		}
		
		var maxColsNum = breakdownHash.size() + 1;
		var graphDataArray = new Array(maxRowsNum+1);
		for (var i = 0; i <= maxRowsNum; i++) {
		  graphDataArray[i] = new Array(maxColsNum);
		}
		
		
		graphDataArray[0][0] = 'x';
		var dataArrayColIndex = 1;
		breakdownHash.each(function(item){
			var dataArrayRowIndex = 0;
			var inpArrayRowIndex = item.value;
			var breakDownCategory = item.key;
			var breakDown = breakDownCategory;
			var tempBrkDn = breakDownCategory.split("|");
			if(tempBrkDn.length>1)
				breakDown = tempBrkDn[1];
			graphDataArray[dataArrayRowIndex][dataArrayColIndex] = breakDown;
			dataArrayRowIndex++;
			siteHash.each(function(entry){
				var siteName = entry.key;
				var inpArrayColIndex = entry.value;
				graphDataArray[dataArrayRowIndex][0] = siteName;
				var breakDownValue = _2DResultsArray[inpArrayRowIndex][inpArrayColIndex];
				if(breakDownValue == " ")
					breakDownValue = "Not Provided";
				graphDataArray[dataArrayRowIndex][dataArrayColIndex] = breakDownValue;
				dataArrayRowIndex++;
			});
			dataArrayColIndex++;
		});
		
		return graphDataArray;
	}
	else
		return null;
}

/*****************************************************************************************************
   @Function i2b2.CRC.view.graphs.parseInputIntoArray(sInputString, isMultiSite)
   @Input (String *text from query status*, Boolean false = single site, true = multiple site)
   @Output Create a two dimensional array out of these strings called asInputFragments
   Each array element is a six element array of 0 "query name", 1 "title", 2 "site", 3 "element name", 4 quantity, 5 "sentence"
   for example, one element would be ["Circulatory sys@20:21:19", "Age patient breakdown", "MGH" "0-9 years old", 0, "0-9 years old: 0"]
   
*****************************************************************************************************/
i2b2.CRC.view.graphs.parseInputIntoArray = function(sInputString, isMultiSite) {
var sCheckForNothing = "something";  // this gets checked to be a zero length string
	try {
		var old_demo = false;
		if (sInputString === undefined || sInputString === null || sInputString === "") throw ("ERROR - sInputString in function parseInputIntoArray is empty");
		var asInputFragments = [[]];
		if (!isMultiSite) {
			var asTempArray = [];
			var sLatestTitle, sLatestQueryName, sLatestElementName, iLatestQuantity, sLatestSite;
			// process input one line at a time to look for the start of a block.
			// you know it because it has two \"'s
			// begin your parsing by separating into an array of sentences that were delimited with \n
			var asInputSentences = sInputString.split("\n");
			var iFragmentArrayCounter = 0;
			for(var i = 0; i < asInputSentences.length; i++) {
				if (asInputSentences[i].indexOf("for") > 0) { 
					asTempArray = asInputSentences[i].split("for");
					sLatestTitle = asTempArray[0];
					sLatestQueryName = asTempArray[1];
					sLatestSite = ".";
				} else if (asInputSentences[i].indexOf(":") > 0) {
					asTemp2Array = asInputSentences[i].split(":");
					sLatestElementName = asTemp2Array[0];
					iLatestQuantity = asTemp2Array[1];
					asInputFragments[iFragmentArrayCounter] = new Array (6);
					asInputFragments[iFragmentArrayCounter][0] = sLatestQueryName;
					asInputFragments[iFragmentArrayCounter][1] = sLatestTitle;
					asInputFragments[iFragmentArrayCounter][2] = sLatestSite;
					asInputFragments[iFragmentArrayCounter][3] = sLatestElementName;
					asInputFragments[iFragmentArrayCounter][4] = i2b2.CRC.view.graphs.sValueOfi2b2Text(iLatestQuantity);
					asInputFragments[iFragmentArrayCounter][5] = asInputSentences[i];
					for(var j = 0; j < 6; j++) {
					}
					iFragmentArrayCounter++;
				} else {
				}
			}
		}
		else if (old_demo == true) {  // parsing for old_demo SHRINE strings
			var asTempArray = [];
			var asTemp2Array = [];
			var sLatestTitle, sLatestQueryName, sLatestElementName, iLatestQuantity, sLatestSite;
			// process input one line at a time to look for the start of a block.
			// you know it because it has two \"'s
			// begin your parsing by separating into an array of sentences that were delimited with \n
			var asInputSentences = sInputString.split("\n");
			var iFragmentArrayCounter = 0;
			for (var i = 0; i < asInputSentences.length; i++) {
				if (asInputSentences[i].indexOf("for") > 0) { 
					asTempArray = asInputSentences[i].split("for");
					sLatestTitle = asTempArray[0];
					if (asTempArray[1].indexOf("=") > 0) {
						asTemp2Array = asTempArray[1].split("=");
						sLatestQueryName = asTemp2Array[0];
						sLatestSite = asTemp2Array[1];
					}
					else {
						sLatestQueryName = asTempArray[1];
						sLatestSite = "xxx";
					}
				} else if (asInputSentences[i].indexOf(":") > 0) {
					asTemp2Array = asInputSentences[i].split(":");
					sLatestElementName = asTemp2Array[0];
					iLatestQuantity = asTemp2Array[1];
					//document.write("<br /> Element " + i + " = " + asInputSentences[i]); 
					asInputFragments[iFragmentArrayCounter] = new Array (6);
					asInputFragments[iFragmentArrayCounter][0] = sLatestQueryName;
					asInputFragments[iFragmentArrayCounter][1] = sLatestTitle;
					asInputFragments[iFragmentArrayCounter][2] = sLatestSite;
					asInputFragments[iFragmentArrayCounter][3] = sLatestElementName;
					asInputFragments[iFragmentArrayCounter][4] = iLatestQuantity;
					asInputFragments[iFragmentArrayCounter][5] = asInputSentences[i];
					iFragmentArrayCounter++;
				} else {
				}
			}
		}
		else {  // parsing for SHRINE strings
				
			//Read all the main breakdown categories
			var asInputSentences = sInputString.split("\n");
			 
			var actualBrkDowns = $H();
			for (var i = 0; i < asInputSentences.length; i++) {
				var thisLine = asInputSentences[i];
				if(thisLine && thisLine.toLowerCase().trim().indexOf('breakdown')>0)
				{
					asTemp2Array = thisLine.split(":");                    // Finding a type of breakdown
					mainCategory = asTemp2Array[0].trim();
					actualBrkDowns.set(mainCategory,mainCategory);
				}
			}
			i2b2.CRC.view.graphs.asTitleOfShrineGroup = [];
			actualBrkDowns.each(function(category)
			{
				i2b2.CRC.view.graphs.asTitleOfShrineGroup.push(category.key);
			});
			
			var asInputFragments = [[]];
			try{
				// parsing for SHRINE strings
				var asTempArray = [];
				var asTemp2Array = [];
				var asInputSentencesBySite =[];
				var sLatestTitle, sLatestQueryName, sLatestElementName, iLatestQuantity, sLatestSite, sQueryName;
				var aSiteArray = [];
				var aTempSiteArray = [];
				// process input one line at a time to look for the start of a block.
				// you know it because it has two \"'s
				// begin your parsing by separating into an array of sentences that were delimited with \n
				var iQueryNameIndex = -1;
				
				// look for query title
				for (var i = 0; i < asInputSentences.length; i++) {
					if (asInputSentences[i].indexOf("Finished Query:") !== -1) {
						asTempArray = asInputSentences[i].split('Finished Query:');
						sQueryName = asTempArray[1].trim();
						iQueryNameIndex = i;
						break;
					}
				}
				if (iQueryNameIndex == -1) return;
				sLatestQueryName = sQueryName;
				// look for Hospitals (which are to the right of the query name) and put each hospital into an array
				var iSiteNumber = -1;
				for (var i = iQueryNameIndex+1; i < asInputSentences.length; i++) {
					if (asInputSentences[i].indexOf(sQueryName) !== -1) {
						if (iSiteNumber !== -1) aSiteArray[iSiteNumber] = aTempSiteArray;
						iSiteNumber = iSiteNumber + 1;
						aTempSiteArray = new Array(3);				
						asTempArray = asInputSentences[i].split(sQueryName);
						aTempSiteArray[0] = asTempArray[0].trim();
						aTempSiteArray[1] = i;
					} else {
						aTempSiteArray[2] = i;
					}			
				}
				if (iSiteNumber == -1) return;
				aSiteArray[iSiteNumber] = aTempSiteArray;
				var iBeginIndex;
				var iEndIndex;	
				var iFragmentArrayCounter=0;
				for(var ii = 0; ii <= iSiteNumber; ii++) {
					sLatestSite = aSiteArray[ii][0];
					iBeginIndex = aSiteArray[ii][1];
					iEndIndex = aSiteArray[ii][2];	
					for(var i = iBeginIndex; i < iEndIndex; i++) {
						var a = asInputSentences[i];
						if (asInputSentences[i].indexOf("Patient Count") > -1) {
							asTemp2Array = asInputSentences[i].split(" -");
							sLatestTitle = 'Patient Count';
							sLatestElementName = 'Patient Count';
							iLatestQuantity = asTemp2Array[1];
							asInputFragments[iFragmentArrayCounter] = new Array (6);
							asInputFragments[iFragmentArrayCounter][0] = sLatestQueryName;
							asInputFragments[iFragmentArrayCounter][1] = sLatestTitle;
							asInputFragments[iFragmentArrayCounter][2] = sLatestSite;
							asInputFragments[iFragmentArrayCounter][3] = sLatestElementName;
							asInputFragments[iFragmentArrayCounter][4] = i2b2.CRC.view.graphs.sValueOfSHRINEText(iLatestQuantity);
							asInputFragments[iFragmentArrayCounter][5] = asInputSentences[i];
							iFragmentArrayCounter++; 
						} else if (i2b2.CRC.view.graphs.asTitleOfShrineGroup.indexOf(asInputSentences[i]) > -1) {
							asTemp2Array = asInputSentences[i].split(" ");                    // Finding a type of breakdown
							sLatestTitle = asTemp2Array[1].trim();
							sCheckForNothing = asTemp2Array[1];
							
						} else if (asInputSentences[i].indexOf(" -") > -1) {                   // parsing the values
							//Lets make sure the - is not part of query name
							if (asInputSentences[i].indexOf('"') < 0){
								asTemp2Array = asInputSentences[i].split(" -");
								sLatestElementName = asTemp2Array[0];
								iLatestQuantity = asTemp2Array[1];
								if (i2b2.CRC.view.graphs.asTitleOfShrineGroup.indexOf(sLatestElementName.toLowerCase().trim()) > -1) {
									sLatestTitle = sLatestElementName.trim();
									sCheckForNothing = iLatestQuantity;
								}
								asInputFragments[iFragmentArrayCounter] = new Array (6);
								asInputFragments[iFragmentArrayCounter][0] = sLatestQueryName;
								asInputFragments[iFragmentArrayCounter][1] = sLatestTitle;
								asInputFragments[iFragmentArrayCounter][2] = sLatestSite;
								asInputFragments[iFragmentArrayCounter][3] = sLatestElementName;
								asInputFragments[iFragmentArrayCounter][4] = i2b2.CRC.view.graphs.sValueOfSHRINEText(iLatestQuantity);
								asInputFragments[iFragmentArrayCounter][5] = asInputSentences[i];
								iFragmentArrayCounter++;
							}
						} else if ((asInputSentences[i].indexOf("ERROR") > -1) || (asInputSentences[i].indexOf("Still Processing Request") > -1) || (asInputSentences[i].indexOf("Results not available") > -1)) {  
							asTemp2Array = asInputSentences[i].split(" : ");
							sLatestError = asTemp2Array[0];
							iLatestErrorMsg = asTemp2Array[1];
							asInputFragments[iFragmentArrayCounter] = new Array (6);
							asInputFragments[iFragmentArrayCounter][0] = sLatestQueryName;
							asInputFragments[iFragmentArrayCounter][1] = sLatestError;
							asInputFragments[iFragmentArrayCounter][2] = sLatestSite;
							asInputFragments[iFragmentArrayCounter][3] = iLatestErrorMsg;
							asInputFragments[iFragmentArrayCounter][4] = '';
							asInputFragments[iFragmentArrayCounter][5] = '';
							iFragmentArrayCounter++;
						} else {
							//Not required to parse
						} // and of fragment processing if statement
						//document.write("<br> Element " + i + " = " + sLatestTitle + " " + sLatestQueryName + " " + sLatestElementName + " " + iLatestQuantity);
					} //
				} // end of for loop that processes sites in SHRINE
			}
			catch(err)
			{
				console.error(err);
			}
		} // end of if statement that directs to single or multisite
		
		return asInputFragments
	}
	catch(err) {
		console.error(err);
	}
} // END of function i2b2.CRC.view.graphs.parseInputIntoArray


/*********************************************************************************
   FUNCTION graph_singlesite_patient_number
   Fills in the Div for a single patient number display
**********************************************************************************/
function graph_singlesite_patient_number(sDivName, sBreakdownType, asInputFragments) {
	try {
		if (sBreakdownType === undefined || sBreakdownType === null) throw ("ERROR - sBreakdownType in function graph_patient_breakdown is null");
		var asBreakdownArray = [[]];
		var iBreakdown = 0;

		// for loop to only pull out data from the breakdown type specified in sBreakdownType variable 
		for (var i = 0; i < asInputFragments.length; i++) {
			if (asInputFragments[i][1].toLowerCase().trim() === sBreakdownType.toLowerCase().trim()) {
				//document.write("<br /> OK? " + i + " = " + asInputFragments[i][0]);
				asBreakdownArray[iBreakdown] = new Array (2);
				asBreakdownArray[iBreakdown][0] = asInputFragments[i][3];
				asBreakdownArray[iBreakdown][1] = asInputFragments[i][4];
				iBreakdown++;
			} else {
				//document.write("<br /> ERROR? " + i + " = " + asInputFragments[i][0]);
			}
		}
		// establish style and draw out containing Div
		var sDivStyle = "font-family: Verdana, Geneva, sans-serif;" 
				+ "font-size: 12px;"
				+ "text-align: center;"
				+ "vertical-align: middle;"
				+ "background-color: white;"
				+ "width: 100%;";
		document.getElementById(sDivName).setAttribute("style",sDivStyle);
		// establish table in Div and set up its style.
		var sDisplayNumber = i2b2.CRC.view.graphs.sTexti2b2Value(asBreakdownArray[0][1]);
		var sTableHtml = '<table style="width: 400px; margin-left: auto; margin-right: auto;">' +
							'<tr style="background-color: white">' +
								'<td style="color: red; text-align: center; vertical-align: middle;">&nbsp</td>' +
							'</tr>' +
							'<tr style="background-color: #B0C4DE">' +
								'<td style="color: black; text-align: center; vertical-align: middle;">'+sBreakdownType+'</td>' +
							'</tr>' +
							'<tr style="background-color: #B0C4DE">' +
								'<td style="color: darkblue; text-align: center; vertical-align: middle; font-size: 45px">'+sDisplayNumber+'</td>' +
							'</tr>' +
							'<tr style="background-color: #B0C4DE">' +
								'<td style="color: black; text-align: center; vertical-align: middle;">For Query '+asInputFragments[0][0]+'</td>' +
							'</tr>' +
							'<tr style="background-color: white">' +
								'<td style="color: red; text-align: center; vertical-align: middle;">&nbsp</td>' +
							'</tr>' +
							'</table>';
		document.getElementById(sDivName).innerHTML = sTableHtml;
	}
	catch(err) {
		console.error(err);
	}
} // END of function graph_single_patient_number


/*********************************************************************************
   FUNCTION graph_singlesite_patient_breakdown
   function where the dataset is displayed
**********************************************************************************/
function graph_singlesite_patient_breakdown(sDivName,sBreakdownType,asInputFragments) {

	try {
		if (sBreakdownType === undefined || sBreakdownType === null) throw ("ERROR 101 in graph_patient_breakdown, sBreakdownType is null");
		var asBreakdownArray = [[]];
		var iBreakdown = 0;
		// for loop to only pull out data from the breakdown type specified in sBreakdownType variable
		// for multiple sites make a data array for each site
		for (var i = 0; i < asInputFragments.length; i++) {
			if (asInputFragments[i][1].toLowerCase().trim() === sBreakdownType.toLowerCase().trim()) {
				//console.log("IN> " + i + " = " + asInputFragments[i][2]);
				asBreakdownArray[iBreakdown] = new Array (3);
				asBreakdownArray[iBreakdown][0] = asInputFragments[i][2]; // site
				asBreakdownArray[iBreakdown][1] = asInputFragments[i][3]; // text
				asBreakdownArray[iBreakdown][2] = asInputFragments[i][4]; // number
				iBreakdown++;
			} else {
				//console.log("OUT> " + i + " = " + asInputFragments[i][0]);  // items that were left out
			}
		}
		// the text 'patient breakdown' is removed and remainder trimmed
		var sBreakdownText = "";
		var iPbLocation = sBreakdownType.toLowerCase().indexOf(" patient breakdown");
		if (iPbLocation != -1) {
			sBreakdownText = sBreakdownType.substring(0,iPbLocation);
			//console.log(sBreakdownText + "|");
		} else {
			sBreakdownText = sBreakdownType;
			//console.log(sBreakdownText + "it is");
		}
		// function where the dataset arrays are created:
		var iBreakdown = 1;
		var c3xaxis = new Array();
		var c3values = new Array();
		c3xaxis[0] = 'x';
		c3values[0] = sBreakdownText;
		for (var i = 0; i < asInputFragments.length; i++) {
			if (asInputFragments[i][1].toLowerCase().trim() === sBreakdownType.toLowerCase().trim()) {
				//document.write("<br /> OK? " + i + " = " + asInputFragments[i][0]);
				c3xaxis[iBreakdown] = asInputFragments[i][3];
				c3values[iBreakdown] = asInputFragments[i][4];
				iBreakdown++;
			} else {
				//document.write("<br /> ERROR? " + i + " = " + asInputFragments[i][0]);
			}
		}
			
		// Trying out some C3
		var graph_color = 'darkblue';
		//var graph_color = 'hsl(' + Math.floor( 360 * Math.random() ) + ', 85%, 55%)'; // random color
		if(!(typeof c3 === 'undefined')){
			var chart = c3.generate({
				bindto: '#' + sDivName,
				size: { 
					//width: 535,
					height: 200
				},
				data: {
					x: 'x',
					columns: [
						c3xaxis,
						c3values
					],
					type: 'bar',
					color: function (color, d) {return graph_color;},
					labels: true
				},
				legend: {
					//position: 'inset'
					position: 'right'
				},
				axis: {
					x: {
						type: 'category',
						tick: {
							rotate: 25
						},
						height: 45
					},
					y: {
						label: {
							text: 'Number of Patients',
							//position: 'outer-middle',
							position: 'outer-bottom'
						}
					}
				},
				bar: {
					width: {
						ratio: 0.75 // this makes bar width 75% of length between ticks
					}
				}
			});
		}
	}
	catch(err) {
		console.error(err);
	}
}; // end of function graph_singlesite_patient_breakdown


/*********************************************************************************
   FUNCTION graph_multiplesite_patient_number
   Fills in the Div for multiple patient number display
**********************************************************************************/
function graph_multiplesite_patient_number(sDivName,sBreakdownType,asInputFragments,maxHeight) {
	try 
	{
		if (sBreakdownType === undefined || sBreakdownType === null) throw ("ERROR - sBreakdownType in function graph_patient_breakdown is null");
		var asBreakdownArray = [[]];
		var iBreakdown = 0;
		// for loop to only pull out data from the breakdown type specified in sBreakdownType variable
		// for multiple sites make a data array for each site
		for (var i = 0; i < asInputFragments.length; i++) {
			if (asInputFragments[i][1].toLowerCase().trim() === sBreakdownType.toLowerCase().trim()) {
				//console.log("IN> " + i + " = " + asInputFragments[i][2]);
				asBreakdownArray[iBreakdown] = new Array (3);
				asBreakdownArray[iBreakdown][0] = asInputFragments[i][2]; // site
				asBreakdownArray[iBreakdown][1] = asInputFragments[i][3]; // text
				asBreakdownArray[iBreakdown][2] = asInputFragments[i][4]; // number
				iBreakdown++;
			} else {
				//console.log("OUT> " + i + " = " + asInputFragments[i][0]);  // items that were left out
			}
		}
		// function where the dataset arrays are created:
		var c3values = new Array();
		var allCountsZero = true;
		for (var i = 0; i < asBreakdownArray.length; i++) {
			//console.log("Element " + i + " = " + asBreakdownArray[i][0] + " " + asBreakdownArray[i][2]);
			c3values[i] = new Array(2);
			c3values[i][0] = asBreakdownArray[i][0].trim() + " " + asBreakdownArray[i][1].trim();
			var patCount = Number(asBreakdownArray[i][2]);
			c3values[i][1] = patCount;
			if(patCount>0)
				allCountsZero = false;
		}
		// C3 that makes pie chart
		if(allCountsZero)
		{
			jQuery(sDivName).html("Not enough patients were returned to render the graph.");
		}
		else
		{
			var graphHeight = 300;
			if(maxHeight)
				graphHeight = maxHeight;
			if(!(typeof c3 === 'undefined')){
				var chart = c3.generate({
					bindto: sDivName,
					size: { 
						//width: 535,
						height: graphHeight
					},
					data: {
						columns: c3values,
						type: 'pie'
					},
					pie: {
						label: {
						format: d3.format('^g,') 
						}
					},
					legend: {
						position: 'bottom'
					},
					axis: {
						x: {
							type: 'category',
							tick: {
								rotate: 25
							},
							height: 45
						},
						y: {
							label: {
								text: 'Number of Patients',
								position: 'outer-bottom'
							}
						}
					},
					bar: {
						width: {
							ratio: 0.75 // this makes bar width 75% of length between ticks
						}
					}
				});
			}
		}
	}
	catch(err) {
		console.error(err);
	}
}; // end of function graph_multiplesite_patient_number


/*********************************************************************************
   FUNCTION graph_multiplesite_patient_breakdown
   function where the dataset is displayed
**********************************************************************************/
function graph_multiplesite_patient_breakdown(sDivName,asUniqueBreakdownSites,c3dataarray,maxHeight,maxWidth) {
	try {
		if(!(typeof c3 === 'undefined')){
			var graphHeight = 300;
			if(maxHeight)
				graphHeight = maxHeight;
			var graphWidth = 1000;
			if(maxWidth)
				graphWidth = maxWidth;
			var chart = c3.generate({
				bindto: sDivName,
				size: { 
					// width: 1000,
					height: graphHeight
				},
				data: {
					x: 'x',
					columns: c3dataarray,
					type: 'bar',
					groups: [asUniqueBreakdownSites]
				},
				padding: {
					left: 60,
					bottom: 40
				},
				legend: {
					position: 'bottom'
				},
				axis: {
					x: {
						type: 'category',
						tick: {
							rotate: 25
						},
						height: 45
					},
					y: {
						label: {
							text: 'Number of Patients'
							// position: 'outer-bottom'
						}
					}
				},
				bar: {
					width: {
						ratio: 0.50 // this makes bar width 75% of length between ticks
					}
				}
			});
		}
	}
	catch(err) {
		console.error(err);
	}
}; // end of function graph_multiplesite_patient_breakdown

/*********************************************************************************
   FUNCTION ie
   function to test for the version of internet explorer
   usage if ie < 9 then ...
**********************************************************************************/
var ie = (function(){

    var undef,
        v = 3,
        div = document.createElement('div'),
        all = div.getElementsByTagName('i');

    while (
        div.innerHTML = '<!--[if gt IE ' + (++v) + ']><i></i><![endif]-->',
        all[0]
    );

    return v > 4 ? v : undef;

}());

/*********************************************************************************
   FUNCTION bisGTIE8 
   function to specifically test for internet explorer gt 8 or any other browser 
	which returns "true"
   usage if bisGTIE8 then ...
**********************************************************************************/
i2b2.CRC.view.graphs.bisGTIE8 = (function(){
	try {
		if ( document.addEventListener ) {
			//alert("you got IE9 or greater (or a modern browser)");
			return true;
		}
		else {
			return false;
		}
	}
	catch (e) {
		return false;
	}
}());

/*********************************************************************************
   FUNCTION sValueOfi2b2Text
   Return a INTEGER number that was obfuscated = MAKES IT ZERO
**********************************************************************************/

i2b2.CRC.view.graphs.sValueOfi2b2Text = function(sValue) {
	try {
		if (sValue === undefined || sValue === null || sValue === "") {
			iValue = "undefined in";
			return iValue;
		}
		if (i2b2.PM.model.isObfuscated) {
			var asTempArray = [];
			if (sValue.toLowerCase().trim() == i2b2.CRC.view.graphs.sObfuscatedText.toLowerCase().trim()) {
				iValue = "0";
			}
			else {
				if (sValue.indexOf(i2b2.CRC.view.graphs.sObfuscatedEnding) > 0) {
					var asTempArray = sValue.split(i2b2.CRC.view.graphs.sObfuscatedEnding);
					iValue = asTempArray[0];
				}
				else {
					iValue = sValue;
				}
			}
		}
		else {
			iValue = sValue;
		}
		function isNumber(obj) {return ! isNaN(obj-0) && obj; };
		if (!isNumber(iValue)) {
			iValue = "undefined #";
			return iValue;
		}
		return iValue;
	}
	catch(err) {
		console.error(err);
	}
} // END of function sValueOfi2b2Text

/*********************************************************************************
   FUNCTION sValueOfSHRINEText
   Return a INTEGER number that was obfuscated = MAKES IT ZERO
**********************************************************************************/

i2b2.CRC.view.graphs.sValueOfSHRINEText = function(sValue) {
	try {
		if (sValue === undefined || sValue === null || sValue === "") {
			iValue = "undefined in";
			return iValue;
		}
		if (true) { // for testing
			var asTempArray = [];
			if (sValue.toLowerCase().trim() == i2b2.CRC.view.graphs.sObfuscatedSHRINEText.toLowerCase().trim()) {
				iValue = "0";
			}
			else {
				if (sValue.indexOf(i2b2.CRC.view.graphs.sObfuscatedSHRINEEnding) > 0) {
					//alert(i2b2.CRC.view.graphs.sObfuscatedSHRINEEnding);
					var asTempArray = sValue.split(i2b2.CRC.view.graphs.sObfuscatedSHRINEEnding);
					//alert(asTempArray);
					iValue = asTempArray[0].trim();
				}
				else {
					iValue = sValue;
				}
			}
		}
		else {
			iValue = sValue;
		}
		function isNumber(obj) {return ! isNaN(obj-0) && obj; };
		if (!isNumber(iValue)) {
			iValue = "undefined #";
			return iValue;
		}
		return iValue;
	}
	catch(err) {
		console.error(err);
	}
} // END of function sValueOfSHRINEText

/*********************************************************************************
   FUNCTION sTexti2b2Value
   Return a TEXT number that has obfuscated blurring - FOR DISPLAY ONLY
**********************************************************************************/

i2b2.CRC.view.graphs.sTexti2b2Value = function(iValue) {
	try {
		if (iValue === undefined || iValue === null || iValue === "") {
			sValue = "undefined";
			return sValue;
		}
		function isNumber(obj) {return ! isNaN(obj-0) && obj; };
		if (!isNumber(iValue)) {
			sValue = "undefined";
			return sValue;
		}		
		if (iValue >= i2b2.CRC.view.graphs.iObfuscatedFloorNumber) {
			if (i2b2.PM.model.isObfuscated) {
				sValue = iValue+i2b2.CRC.view.graphs.sObfuscatedEnding;
			} else {
				sValue = iValue;
			}
		} else {
			if (i2b2.PM.model.isObfuscated) {
				sValue = i2b2.CRC.view.graphs.sObfuscatedText;
			} else {
				sValue = iValue;
			}
		}
		return sValue;
	}
	catch(err) {
		console.error(err);
	}
} // END of function sTexti2b2Value
			
			
/*********************************************************************************
   FUNCTION clearGraphs
   Clear the previously used Div for the graphs
**********************************************************************************/
i2b2.CRC.view.graphs.clearGraphs = function(sDivNameToClear) {
	try {
		if (sDivNameToClear === undefined || sDivNameToClear === null || sDivNameToClear === "") 
			throw ("ERROR 291 - i2b2.CRC.view.graphs.sNameOfPreviousDiv in function clearGraphs is null");
		//Clear Divs in the original div for the charts
		var oClearDiv = document.getElementById(sDivNameToClear);
		oClearDiv.innerHTML = "";
	}
	catch(err) {
		console.error(err);
	}
} // END of function clearGraphs

/*********************************************************************************
   FUNCTION clearDivForIE8
   Clear Div and put message in the middle that "The Graphs Cannot Display in IE8"
**********************************************************************************/
i2b2.CRC.view.graphs.aDivForIE8 = function(sDivNameToClear) {
	try {
		if (sDivNameToClear === undefined || sDivNameToClear === null || sDivNameToClear === "") 
			throw ("ERROR 291 - i2b2.CRC.view.graphs.sNameOfPreviousDiv in function clearGraphs is null");
		// Clear Divs in the original div for the charts
		var oClearDiv = document.getElementById(sDivNameToClear);		
		var child = document.createElement("div");
		child.setAttribute("id","IE_Div");
		child.setAttribute("width","auto");
		oClearDiv.appendChild(child);

		// establish style and draw out containing Div
		var sDivStyle = "font-family: Verdana, Geneva, sans-serif;" 
				+ "font-size: 12px;"
				+ "text-align: center;"
				+ "vertical-align: middle;"
				+ "background-color: white;"
				+ "width: 100%;";
		child.setAttribute("style",sDivStyle);
		// establish table in Div and set up its style.
		var sTableHtml = '<table style="width: 400px; margin-left: auto; margin-right: auto;">' +
							'<tr style="background-color: white">' +
								'<td style="color: red; text-align: center; vertical-align: middle;">&nbsp</td>' +
							'</tr>' +
							'<tr style="background-color: white">' +
								'<td style="color: darkblue; text-align: center; vertical-align: middle; font-size: 20px">Graph Results is not supported for this version</td>' +
							'</tr>' +
							'<tr style="background-color: white">' +
								'<td style="color: darkblue; text-align: center; vertical-align: middle; font-size: 20px">of Internet Explorer. In order to display the graphs in </td>' +
							'</tr>' +                        
							'<tr style="background-color: white">' +
								'<td style="color: darkblue; text-align: center; vertical-align: middle; font-size: 20px">Internet Explorer you will need to use version 11 or higher.</td>' +
							'</tr>' + 
							'<tr style="background-color: white">' +
								'<td style="color: red; text-align: center; vertical-align: middle;">&nbsp</td>' +
							'</tr>' +
							'</table>';
		child.innerHTML = sTableHtml;
	}
	catch(err) {
		console.error(err);
	}
} // END of function clearDivForIE8

console.timeEnd('execute time');
console.groupEnd();