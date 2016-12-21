/**
 * @projectDescription	View controller for the download data window (which is a GUI-only component of the CRC module).
 * @inherits 	i2b2.CRC.view
 * @namespace	i2b2.CRC.view.DownloadData
 * @author 		Bhaswati Ghosh
 * @version 	1.3
 * ----------------------------------------------------------------------------------------
 */
 
console.group('Load & Execute component file: CRC > view > DownloadData');
console.time('execute time');


// create and save the screen objects
i2b2.CRC.view.downloadData = new i2b2Base_cellViewController(i2b2.CRC, 'downloadData');
i2b2.CRC.view.downloadData.visible = false;

i2b2.CRC.view.downloadData.show = function() {
	i2b2.CRC.view.downloadData.visible = true;
	$('crcDownloadDataBox').show();
}
i2b2.CRC.view.downloadData.hide = function() {
	i2b2.CRC.view.downloadData.visible = false;
	$('crcDownloadDataBox').hide();
}

i2b2.CRC.view.downloadData.hideDisplay = function() {
	$('infoDownloadStatusData').hide();
}

i2b2.CRC.view.downloadData.showDisplay = function() {
	var targs = $('infoDownloadStatusData').parentNode.parentNode.select('DIV.tabBox.active');
	// remove all active tabs
	targs.each(function(el) { el.removeClassName('active'); });
	// set us as active
	$('infoDownloadStatusData').parentNode.parentNode.select('DIV.tabBox.tabDownloadData')[0].addClassName('active');
	
	$('infoQueryStatusText').hide();
	$('infoQueryStatusChart').hide();
	$('infoQueryStatusReport').hide();
	$('infoDownloadStatusData').show();
	
	var output = "";
	
	if(typeof i2b2.CRC.view.downloadData.active_qm_id === 'undefined'){
	
	} else {
		output = "<div style=\"padding:15px;text-align:center;font-size:16px;\"><img src=\"js-i2b2/cells/plugins/biobankportal/BiobankDatafile/assets/csv_icon.png\" align=\"absmiddle\"/> You can download the de-identified data for this query as an Excel/CSV file. ";
		output += "<input onclick=\"loadDownloadData();i2b2.BiobankDatafile.queryAutoDropped(i2b2.CRC.view.downloadData.active_qm_id);\" type=\"button\" class=\"BiobankDatafile-button\" value=\"Proceed to the Download\">";
		output += "</div>";
	
	}
	
	$('infoDownloadStatusData').innerHTML = output;
}

i2b2.CRC.view.downloadData.createCSV = function() {
	var formattedResObj = i2b2.CRC.ctrlr.currentQueryResults;
	var resString = formattedResObj.resultString;
	if(resString && resString.length>0)
	{
		var asInputFragments = i2b2.CRC.view.graphs.parseInputIntoArray(resString,true);
		i2b2.CRC.view.downloadData.getFormattedResults(asInputFragments);
		i2b2.CRC.view.downloadData.getCSVFromResultsHash();
	}
}

allSiteNamesHash = $H();  //This hash keeps all the facility names.
finalOrderdSiteNamesHash = $H();  //In this hash we order the names  of all facilities and assign them an index number
											 
resultsHash = $H();  //This hashtable keeps {'breakdown|category',{{'facility Name',{integer value,obfuscated value}}} pairs

errorsHash = $H();  //This hastable keeps {facility name, error message} pairs for all sites that returns error

finalResultsArray = new Array([]);  // This is a 2-D array. Keeps data in tabular format where the columns are the sites and rows are the category of results.This array wiil be used to create the CSV table easily.

breakdownHash = $H();  //This hashtable will keep al breakdown categories in lowercase letters. This will help resolve case sensitivity issues for same categories


i2b2.CRC.view.downloadData.getFormattedResults = function(inpArray) {
	try{
		//Clear all the hashtables
		allSiteNamesHash = $H();
		resultsHash = $H();
		errorsHash = $H();
		breakdownHash = $H();
		
		//Populate the facility names hash and results hash tables
		inpArray.each(function(item){
			var qryName = item[0];
			var brkDownCategoryOrError = item[1];
			var facilityName = item[2];
			var brkDownLabelorErrorMsg = item[3];
			var brkDownValue = item[4];
			var obfuscatedEntry = item[5];
			var obfuscatedVal = item[5];
			if(obfuscatedVal)
			{
				var tempObfuscated = obfuscatedEntry.split(" - ");
				if(tempObfuscated && tempObfuscated.length>1)
					obfuscatedVal = tempObfuscated[1];
			}
			
			var errorEntry = false;
			if(brkDownCategoryOrError && brkDownCategoryOrError.indexOf("ERROR") > -1)
				errorEntry = true;
			if(brkDownCategoryOrError && brkDownCategoryOrError.indexOf("Still Processing Request") > -1)
				errorEntry = true;
			if(brkDownCategoryOrError && brkDownCategoryOrError.indexOf("Results not available") > -1)
				errorEntry = true;
			
			if(facilityName && qryName)
				allSiteNamesHash.set(facilityName.trim(),qryName.trim());
			
			//Is this a result entry or error entry
			if(errorEntry)
			{
				if(facilityName && qryName)
					errorsHash.set(facilityName.trim(),brkDownLabelorErrorMsg.trim());
			}
			else  //this is a result entry
			{
				if(brkDownCategoryOrError && brkDownLabelorErrorMsg && obfuscatedVal)
				{
					var brkDownCategory = brkDownCategoryOrError.trim();
					var brkDownLabel = brkDownLabelorErrorMsg.trim();
					var keyId = brkDownCategory + "|" + brkDownLabel;
					if(resultsHash.get(keyId))
					{
						var thisKeyDict = resultsHash.get(keyId);
						
						//Save this site entry in proper breakdown hash
						thisKeyDict.set(facilityName,{'integerValue' : brkDownValue, 'obfuscated':obfuscatedVal});
					}
					else
					{
						//Create a dictionary for the site in the very first entry
						var thisKeyDict = $H();
						
						//Create a dictionary for break down category for the very first site in input array
						var thisSiteDict = $H();
						thisSiteDict.set(facilityName,{'integerValue' : brkDownValue, 'obfuscated':obfuscatedVal});
						
						resultsHash.set(keyId,thisSiteDict);
					}
				}
			}
		});
		
		//We have all our required data hashed. Now lets make a tabular format from it.
		var maxRowsNum = resultsHash.size();
		var maxColsNum = allSiteNamesHash.size();

		finalResultsArray = new Array(maxRowsNum+1);
		for (var i = 0; i <= maxRowsNum; i++) {
		  finalResultsArray[i] = new Array(maxColsNum+1);
		}
		
		for(var i = 0 ; i<=maxRowsNum ; i++)
		{
			for(var j = 0 ;j<=maxColsNum; j++)
			{
				finalResultsArray[i][j]=' ';
			}
		}
		
		finalOrderdSiteNamesHash = $H(); 
		var count = 1;
		
		allSiteNamesHash.keys().sort().each(function(orderedEntry)
		{
			finalOrderdSiteNamesHash.set(orderedEntry,count);
			count++;
		});
		
		//Fill in the first row of the 2-D array with all site names in alphabetic order
		finalResultsArray[0][0]='Site names';
		finalOrderdSiteNamesHash.each(function(entry)
		{
			var col = entry.value;
			var colEntry = entry.key;
			finalResultsArray[0][col]=colEntry;
		});
		
		//Fill in the first column of the 2-D array with all the break down categories
		var i=0;
		var rowIndex = 0;
		
		var temporaryCatgryArr = new Array(maxRowsNum);
		
		resultsHash.keys().sort().each(function(breakdown){
			try{
				var patCountRow = false;
				var lowerCaseBrkDown = breakdown.toLowerCase();  //Resolving case issue for same breakdown fact
				if(breakdown.indexOf("Patient Count") > -1)
					patCountRow = true;
				if(!patCountRow)
				{
					if(!(breakdownHash.get(lowerCaseBrkDown))){
						breakdownHash.set(lowerCaseBrkDown,rowIndex+2);
						rowIndex++;
					}
				}
				else
				{
					//Reserve the 2nd row for patient count data
					finalResultsArray[1][0] = breakdown;
					breakdownHash.set(lowerCaseBrkDown,1);
				}
				temporaryCatgryArr[i++] = breakdown;
			}
			catch(err1)
			{
				console.error("Error while sorting the breakdown categories : " + err1);
			}
		});
		
		//Fill in the rest of data in the array
		for(var i = 0 ; i < temporaryCatgryArr.length ; i++)
		{
			try{
				var breakdownCat = temporaryCatgryArr[i];
				var lowerCaseBrkDown = breakdownCat.toLowerCase();
				var rowIndex = breakdownHash.get(lowerCaseBrkDown);
				if(rowIndex){
					var patCountRow = false;
					if(breakdownCat.indexOf("Patient Count") > -1)
						patCountRow = true;
					if(resultsHash.get(breakdownCat))
					{
						var thisBrkDnHash = resultsHash.get(breakdownCat);
						if(patCountRow)  //Fill in the 2nd row with patient count data
							finalResultsArray[1][0] = 'All Patients';
						else
							finalResultsArray[rowIndex][0] = breakdownCat;
						
						thisBrkDnHash.each(function(item){
							var siteName = item.key;
							var siteIndex = finalOrderdSiteNamesHash.get(siteName);
							var integerVal = item.value.integerValue;
							finalResultsArray[rowIndex][siteIndex] = integerVal;
						});
					}
					else
					{
						var err = "Caltegory " + breakdown + " was not found in resullt hashtable";
						console.error(err);
					}
				}
			}
			catch(err1)
			{
				console.error("Error while putting together the results table : " + err1);
				continue;
			}
		}
		return finalResultsArray;
	}
	catch(err)
	{
		console.error(err);
		return null;
	}
};

i2b2.CRC.view.downloadData.getCSVFromResultsHash = function() {
	var numberOfSites = finalOrderdSiteNamesHash.size();
	if(numberOfSites>0)
	{
		var maxRowsNum = breakdownHash.size()+1;
		var maxColNumber = numberOfSites+1;
		
		var content = "<img src=\"js-i2b2/cells/CRC/assets/csv.png\"/> <a href='#' onclick='javascript:i2b2.CRC.view.downloadData.exportTableToCSV();return false;'>Download CSV File</a><br/><br/><br/>";
		content += "<table id='resultsTable'>";
		content += "<tr>"+ //Beginning of Site names row
					"<th colspan=\"" + maxColNumber + "\">SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS)</th>"+
					"</tr>";
		
		for(var i = 0 ; i < maxRowsNum ; i++)
		{
			content += "<tr>";
			
			for(var j = 0 ; j < maxColNumber ; j++)
			{
				var thisRowColItem = finalResultsArray[i][j];
				if(thisRowColItem)
				{
					if(i2b2.h.isMinObfuscation(thisRowColItem.trim())) {
						thisRowColItem = 0;
					}
						
					if(i==0 && j==0)
						content += "<td>&nbsp;</td>";
					else{
						if(thisRowColItem==' ')
							content += "<td>&nbsp;</td>";
						else
							content += "<td>" + thisRowColItem + "</td>";	
					}
				}
				else
					content += "<td>&nbsp;</td>";
			}
			content += "</tr>";
		}
					
		content += "</table>";
		jQuery('#infoDownloadStatusData').append(content);
	}
	else
	{}
	
}

i2b2.CRC.view.downloadData.exportTableToCSV = function () {

	var table = jQuery('#resultsTable');
	var filename = 'export.csv';
	var rows = table.find('tr:has(td)');

		// Temporary delimiter characters unlikely to be typed by keyboard
		// This is to avoid accidentally splitting the actual contents
	var tmpColDelim = String.fromCharCode(11); // vertical tab character
	var tmpRowDelim = String.fromCharCode(0); // null character

		// actual delimiter characters for CSV format
	var colDelim = '","';
	var rowDelim = '"\r\n"';

		// Grab text from table into CSV formatted string
	var csv = '"' + jQuery(rows).map(function (i, row) {
			var row = jQuery(row),
				cols = row.find('td');

			return jQuery(cols).map(function (j, col) {
				var col = jQuery(col),
					text = col.text();

				if(i==0 && j==0)
					text = "SHRINE QUERY RESULTS (OBFUSCATED PATIENT COUNTS)";
				else
				{
					if(elementIsEmpty(col))
						text = "";
				}
				text = text.replace(/"/g, '""'); // escape double quotes
				return text.replace(/,/g, '",'); // escape comma inside a field

			}).get().join(tmpColDelim);

		}).get().join(tmpRowDelim)
			.split(tmpRowDelim).join(rowDelim)
			.split(tmpColDelim).join(colDelim) + '"';

	//	Check Browser
	var ua = window.navigator.userAgent;
	var msie = ua.indexOf("MSIE ");
	var trident = ua.indexOf('Trident/');
	var edge = ua.indexOf('Edge/');
	var browserIsIE = false;
	if (msie > 0)  {    // IE 10 or older 
		browserIsIE = true;
	}
	else if (trident > 0) { // IE 11 
		browserIsIE = true;
	}
	else if (edge > 0) { // Edge
		browserIsIE = true;
	}
	else
		browserIsIE = false;
	if(browserIsIE)
	{
		if (window.navigator.msSaveOrOpenBlob) { // IE 10+
			// var fileData = ['\ufeff'+csv];
			// blobObject = new Blob(fileData);
			var blobObject = new Blob([decodeURIComponent(encodeURI(csv))], {
				type: "text/csv;charset=utf-8;"
			  });
			navigator.msSaveOrOpenBlob(blobObject, filename);
			return false;
		} 
		else
		{
			var IEwindow = window.open();
			IEwindow.document.write('sep=,\r\n' + csv);
			IEwindow.document.close();
			var success = IEwindow.document.execCommand('SaveAs', true, filename);
			IEwindow.close();
			if (!success)
                alert("Sorry, your browser does not support this feature");
		}
	}
	else
	{
		var mylink = document.createElement('a');
		var csvData = 'data:application/csv;charset=utf-8,' + encodeURIComponent(csv);
		mylink.download = filename;
		mylink.href = csvData;
		document.body.appendChild(mylink);
		mylink.click();
	}
}

function elementIsEmpty(td) {
    if (td.text == '' || td.text() == ' ' || td.html() == '&nbsp;' || td.is(":not(:visible)")) {
		return true;
	}            
	return false;
}

// ================================================================================================== //


console.timeEnd('execute time');
console.groupEnd();