/**
 * @projectDescription	The Asynchronous Query Status controller (GUI-only controller).
 * @inherits 	i2b2.CRC.ctrlr
 * @namespace	i2b2.CRC.ctrlr.QueryStatus
 * @author		Nick Benik, Griffin Weber MD PhD
 * @version 	1.0
 * ----------------------------------------------------------------------------------------
 * updated 8-10-09: Initial Creation [Nick Benik] 
 */


function cgmUtcDateParser(dateString){
//Date format:  2011-02-21T14:35:03.480-05:00
 try{
	  splitDateAndTime = dateString.split("T");
	  vrDate = splitDateAndTime[0].split("-");
	  vrTime = splitDateAndTime[1].split(":");


	  strYear 	= vrDate[0];
	  strMonth 	= vrDate[1] - 1;
	  strDay 	= vrDate[2];


	/*
	alert("Year: "+ strYear);
	alert("Month: "+ strMonth);
	alert("Day: "+ strDay);*/


	  strHours	= vrTime[0];
	  strMins	= vrTime[1];
	  strSecs	= null;
	  strMills	= null;


	  vSecs 	= vrTime[2].split(".");
	  strSecs  	= vSecs[0];

	  vMills 	= vSecs[1].split("-");
	  strMills	= vMills[0];


	/*
	alert("Hours: "+ strHours);
	alert("Minutes: "+ strMins);
	alert("Seconds: "+ strSecs);
	alert("MilliSeconds: "+ strMills);*/
	 return new Date(strYear, strMonth, strDay, strHours, strMins, strSecs, strMills);
	}
 catch(e){
	return null;
 }
}

i2b2.CRC.ctrlr.QueryStatus = function(dispDIV) { this.dispDIV = dispDIV; };

i2b2.CRC.ctrlr.currentQueryResults = null;

function trim(sString) {
	while (sString.substring(0,1) == '\n') {
		sString = sString.substring(1, sString.length);
	}

	while (sString.substring(sString.length-1, sString.length) == '\n') {
		sString = sString.substring(0,sString.length-1);
	}

	return sString;
}



i2b2.CRC.ctrlr.QueryStatus.prototype = function() {
	var private_singleton_isRunning = false;
	var private_startTime = false;
	var private_refreshInterrupt = false;

	function private_refresh_status() {
		
				// callback processor to check the Query Instance
		var scopedCallbackQRSI = new i2b2_scopedCallback();
		scopedCallbackQRSI.scope = self;
		
		
		
		scopedCallbackQRSI.callback = function(results) {
			if (results.error) {
				alert(results.errorMsg);
				return;
			} else {
				// find our query instance

				var ri_list = results.refXML.getElementsByTagName('query_result_instance');
				var l = ri_list.length;
				for (var i=0; i<l; i++) {
					var temp = ri_list[i];
					var description = i2b2.h.XPath(temp, 'descendant-or-self::description')[0].firstChild.nodeValue;
					self.dispDIV.innerHTML += "<div style=\"clear: both;  padding-top: 10px; font-weight: bold;\">" + description + "</div>";					

				} 
				var crc_xml = results.refXML.getElementsByTagName('crc_xml_result');
				l = crc_xml.length;
				for (var i=0; i<l; i++) {			
					var temp = crc_xml[i];
					var xml_value = i2b2.h.XPath(temp, 'descendant-or-self::xml_value')[0].firstChild.nodeValue;

					var xml_v = i2b2.h.parseXml(xml_value);	
						
					var params = i2b2.h.XPath(xml_v, 'descendant::data[@column]/text()/..');
					for (var i2 = 0; i2 < params.length; i2++) {
						var name = params[i2].getAttribute("name");
						if (i2b2.PM.model.isObfuscated) {
							if ( params[i2].firstChild.nodeValue < 4) {
								var value = "<3";	
							} else {
								var value = params[i2].firstChild.nodeValue + "+-" + i2b2.h.getObfuscationValue();
							}
						} else
						{
							var value = params[i2].firstChild.nodeValue;							
						}
						self.dispDIV.innerHTML += "<div style=\"clear: both; margin-left: 20px; float: left; height: 16px; line-height: 16px;\">" + params[i2].getAttribute("column") + ": <font color=\"#0000dd\">" + value  + "</font></div>";
					}
					var ri_id = i2b2.h.XPath(temp, 'descendant-or-self::result_instance_id')[0].firstChild.nodeValue;
				}
				//self.dispDIV.innerHTML += this.dispMsg;
			}
		}

		
		var self = i2b2.CRC.ctrlr.currentQueryStatus;
		// this private function refreshes the display DIV
		var d = new Date();
		var t = Math.floor((d.getTime() - private_startTime)/100)/10;
		var s = t.toString();
		if (s.indexOf('.') < 0) {
			s += '.0';
		}
		
		if (private_singleton_isRunning) {
			self.dispDIV.innerHTML = '<div style="clear:both;"><div style="float:left; font-weight:bold">Running Query: "'+self.QM.name+'"</div>';
			// display the current run duration

			self.dispDIV.innerHTML += '<div style="float:right">['+s+' secs]</div>';
		} else {
			self.dispDIV.innerHTML = '<div style="clear:both;"><div style="float:left; font-weight:bold">Finished Query: "'+self.QM.name+'"</div>';
			self.dispDIV.innerHTML += '<div style="float:right">['+s+' secs]</div>';
			//		self.dispDIV.innerHTML += '<div style="margin-left:20px; clear:both; height:16px; line-height:16px; "><div height:16px; line-height:16px; ">Compute Time: ' + (Math.floor((self.QI.end_date - self.QI.start_date)/100))/10 + ' secs</div></div>';
			//		self.dispDIV.innerHTML += '</div>';
			$('runBoxText').innerHTML = "Run Query";

		}
		self.dispDIV.innerHTML += '</div>';
		if ((!private_singleton_isRunning) && (undefined != self.QI.end_date)){
			self.dispDIV.innerHTML += '<div style="margin-left:20px; clear:both; line-height:16px; ">Compute Time: '+ (Math.floor((self.QI.end_date - self.QI.start_date)/100))/10 +' secs</div>';
		}
		
		var foundError = false;

        for (var i=0; i < self.QRS.length; i++) {
			var rec = self.QRS[i];			
			if (rec.QRS_time) {
				var t = '<font color="';
				switch(rec.QRS_Status) {
					case "ERROR":
						self.dispDIV.innerHTML += '<div style="clear:both; height:16px; line-height:16px; "><div style="float:left; font-weight:bold; height:16px; line-height:16px; ">'+rec.title+'</div><div style="float:right; height:16px; line-height:16px; "><font color="#dd0000">ERROR</font></div>';
	//					self.dispDIV.innerHTML += '<div style="float:right; height:16px; line-height:16px; "><font color="#dd0000">ERROR</font></div>'; //['+rec.QRS_time+' secs]</div>';
						foundError = true;
						break;
					case "COMPLETED":
					case "FINISHED":
						foundError = false;
						//t += '#0000dd">'+rec.QRS_Status;
						break;
					case "INCOMPLETE":
					case "WAITTOPROCESS":
					case "PROCESSING":
						self.dispDIV.innerHTML += '<div style="clear:both; height:16px;line-height:16px; "><div style="float:left; font-weight:bold;  height:16px; line-height:16px; ">'+rec.title+'</div><div style="float:right; height:16px; line-height:16px; "><font color="#00dd00">PROCESSING</font></div>';	
		//				self.dispDIV.innerHTML += '<div style="float:right; height:16px; line-height:16px; "><font color="#00dd00">PROCESSING</font></div>'; //['+rec.QRS_time+' secs]</div>';
						alert('Your query has timed out and has been rescheduled to run in the background.  The results will appear in "Previous Queries"');
						foundError = true;
					
						//t += '#00dd00">'+rec.QRS_Status;
						break;
				}
				t += '</font> ';
				//self.dispDIV.innerHTML += '<div style="float:right; height:16px; line-height:16px; ">'+t+'['+rec.QRS_time+' secs]</div>';
			}
			self.dispDIV.innerHTML += '</div>';
			if (foundError == false) {
				if (rec.QRS_DisplayType == "CATNUM") {

                    //make call to get QRSI.
					i2b2.CRC.ajax.getQueryResultInstanceList_fromQueryResultInstanceId("CRC:QueryStatus", {qr_key_value: rec.QRS_ID}, scopedCallbackQRSI);
				} else if ((rec.QRS_DisplayType == "LIST") && (foundError == false)) {
					self.dispDIV.innerHTML += "<div style=\"clear: both; padding-top: 10px; font-weight: bold;\">" + rec.QRS_Description + "</div>";
				} else if (i2b2.h.isDQ) {
					self.dispDIV.innerHTML += "<div style=\"margin-left:20px; clear:both; height:16px; line-height:16px; \"><div style=\"float:left; height:16px; line-height:16px; \">" + rec.title + "</div><div style=\"float:right; height:16px; line-height:16px; \"><font color=\"#0000dd\">" + rec.QRS_Status + "</font> [" + rec.QRS_time + " secs]</div></div>";
				}
				if (rec.QRS_Type == "PATIENTSET") {
				
					// Check to see if timeline is checked off, if so switch to timeline
					var t2 = $('dialogQryRun').select('INPUT.chkQueryType');
					for (var i=0;i<t2.length; i++) {
						var curItem = t2[i].nextSibling.data;
						if (curItem != undefined)
						{
							curItem = curItem.toLowerCase();
							//curitem = curItem.trim();
						}
						if ((t2[i].checked == true) && (rec.size > 0) && (curItem == " timeline")  
						&& !(i2b2.h.isBadObjPath('i2b2.Timeline.cfg.config.plugin'))
						) {

							i2b2.hive.MasterView.setViewMode('Analysis');
							i2b2.PLUGINMGR.ctrlr.main.selectPlugin("Timeline");
					
							//Process PatientSet
							rec.QM_id = self.QM.id;
							rec.QI_id = self.QI.id;
							rec.PRS_id = rec.QRS_ID;
							rec.result_instance_id = rec.PRS_id;
							var sdxData = {};
							sdxData[0] = i2b2.sdx.Master.EncapsulateData('PRS', rec);							
							i2b2.Timeline.prsDropped(sdxData);
							
							i2b2.Timeline.setShowMetadataDialog(false);
							
							//Process Concepts, put all concepts in one large set
							sdxData = {};
							for (var j2 = 0; j2 < i2b2.CRC.model.queryCurrent.panels.length; j2++) {
							var panel_list = i2b2.CRC.model.queryCurrent.panels[j2]
							var panel_cnt = panel_list.length;
							
							for (var p2 = 0; p2 < panel_cnt; p2++) {
								// Concepts
								for (var i2=0; i2 < panel_list[p2].items.length; i2++) {
									sdxData[0] = panel_list[p2].items[i2];
									i2b2.Timeline.conceptDropped(sdxData);
								}
							}
							}
							//$('Timeline-pgstart').value = '1';
							//$('Timeline-pgsize').value = '10';
							//i2b2.Timeline.pgGo(0);
							i2b2.Timeline.yuiTabs.set('activeIndex', 1);
							
							i2b2.Timeline.setShowMetadataDialog(true);
						}
					} 
				}
			}
		}
		if ((undefined != self.QI.message)  && (foundError == false)) {

			self.dispDIV.innerHTML += '<div style="clear:both; float:left;  padding-top: 10px; font-weight:bold">Status</div>';
			var mySplitResult = self.QI.message.split("<?xml");

			for(i3 = 1; i3 < mySplitResult.length; i3++){

				var xml_v = i2b2.h.parseXml(trim("<?xml " + mySplitResult[i3]));	

				for (var i2 = 0; i2 < xml_v.childNodes.length; i2++) {
					try { 
						if (i2b2.PM.model.isObfuscated) {
							if (i2b2.h.XPath(xml_v, 'descendant::total_time_second/text()/..')[i2].firstChild.nodeValue < 4)
							{
							    var value = "<3";
							} else {
								var value = i2b2.h.XPath(xml_v, 'descendant::total_time_second/text()/..')[i2].firstChild.nodeValue + "+-" + i2b2.h.getObfuscationValue();
							}
						} else
						{
							var value =  i2b2.h.XPath(xml_v, 'descendant::total_time_second/text()/..')[i2].firstChild.nodeValue;							
						}					
					self.dispDIV.innerHTML += '<div style="margin-left:20px; clear:both; line-height:16px; ">' + i2b2.h.XPath(xml_v, 'descendant::name/text()/..')[i2].firstChild.nodeValue + '<font color="#0000dd">: ' + value + ' secs</font></div>';
	
					//self.dispDIV.innerHTML += '<div style="float: left; height: 16px; margin-right: 100px; line-height: 16px;"><font color="#0000dd">: ' + i2b2.h.XPath(xml_v, 'descendant::total_time_second/text()/..')[i2].firstChild.nodeValue + ' secs</font></div>';
					} catch (e) {}
				}
			}
		}

		self.dispDIV.style.display = 'none';
		self.dispDIV.style.display = 'block';

		if (!private_singleton_isRunning && private_refreshInterrupt) {
			// make sure our refresh interrupt is turned off
			try {
				clearInterval(private_refreshInterrupt);
				private_refreshInterrupt = false;
			} catch (e) {}
		}
	}

	
	function private_cancelQuery() {
		if (private_singleton_isRunning) {
			try {
				var self = i2b2.CRC.ctrlr.currentQueryStatus;
				i2b2.CRC.ctrlr.history.queryDeleteNoPrompt(self.QM.id);
					clearInterval(private_refreshInterrupt);
					private_refreshInterrupt = false;
					private_singleton_isRunning = false;
					$('runBoxText').innerHTML = "Run Query";
					self.dispDIV.innerHTML += '<div style="clear:both; height:16px; line-height:16px; text-align:center; color:r#ff0000;">QUERY CANCELLED</div>';
					i2b2.CRC.ctrlr.currentQueryStatus = false; 
			} catch (e) {}	
		}
	}

	function private_startQuery() {
		var self = i2b2.CRC.ctrlr.currentQueryStatus;
		var resultString = ""; //BG

		if (private_singleton_isRunning) {
            return false;
        }

		private_singleton_isRunning = true;
		//BG
		var downloadDataTab = $('infoDownloadStatusData');
		if(downloadDataTab)
			downloadDataTab.innerHTML="";
		i2b2.CRC.ctrlr.currentQueryResults = new i2b2.CRC.ctrlr.QueryResults(resultString);
		//BG
		self.dispDIV.innerHTML = '<b>Processing Query: "'+this.name+'"</b>';
		self.QM.name = this.name; 
		self.QRS = [];
		self.QI = {};
		
		// callback processor to run the query from definition
		this.callbackQueryDef = new i2b2_scopedCallback();
		this.callbackQueryDef.scope = this;

        this.callbackQueryDef.callback = function(results) {

            try
			{
				//if error
				if (results.error) {
					var temp = results.refXML.getElementsByTagName('response_header')[0];
					if (undefined != temp) {
						results.errorMsg = i2b2.h.XPath(temp, 'descendant-or-self::result_status/status')[0].firstChild.nodeValue;
						if (results.errorMsg.substring(0,9) == "LOCKEDOUT")
						{
							results.errorMsg = 'As an "obfuscated user" you have exceeded the allowed query repeat and are now LOCKED OUT, please notify your system administrator.';
						}
					}
					alert(results.errorMsg);
					private_cancelQuery();
					return;
				}

				//query was successful so update global settings.
				clearInterval(private_refreshInterrupt);
				private_refreshInterrupt    =  false;
				private_singleton_isRunning = false;
				resultString = "";  //BG

				//update the ui
				var self = i2b2.CRC.ctrlr.currentQueryStatus;

				// this private function refreshes the display DIV
				var d = new Date();
				var t = Math.floor((d.getTime() - private_startTime)/100)/10;
				var s = t.toString();
				if (s.indexOf('.') < 0) {
					s += '.0';
				}

				
				self.dispDIV.innerHTML = '<div style="clear:both;"><div style="float:left; font-weight:bold">Finished Query: "'+self.QM.name+'"</div>';
				self.dispDIV.innerHTML += '<div style="float:right">['+s+' secs]</div><br/>';
				resultString += 'Finished Query: "' + self.QM.name + '"\n';  //BG
				resultString += '[' + s + ' secs]\n'; //BG
				
				
				$('runBoxText').innerHTML = "Run Query";

				//------------ QI Logic -------------------//
				
				//BG find user id
				var temp = results.refXML.getElementsByTagName('query_master')[0];
				
				//Update the userid element when query is run first time
				var userId = i2b2.h.getXNodeVal(temp,'user_id');
				if(userId)
				{
					var existingUserIdElemList = $$("#userIdElem");
					if(existingUserIdElemList)
					{
						existingUserIdElemList.each(function(existingUserIdElem){
							existingUserIdElem.remove();
						});
					}
					$("crcQueryToolBox.bodyBox").insert(new Element('input',{'type':'hidden','id':'userIdElem','value':userId}));
				}
				//End BG
				
				
				// find our query instance
				var qi_list = results.refXML.getElementsByTagName('query_instance');  //Original code commented by BG
				
				
				var l       = qi_list.length;

				for (var i=0; i<l; i++) {
					var qiNode = qi_list[i];
					var qi_id = i2b2.h.XPath(qiNode, 'descendant-or-self::query_instance_id')[0].firstChild.nodeValue;

					this.QI.message = i2b2.h.getXNodeVal(qiNode, 'message');

					//start date.
					this.QI.start_date = i2b2.h.getXNodeVal(qiNode, 'start_date');
					if (!Object.isUndefined(this.QI.start_date)) {
						this.QI.start_date =  new Date(this.QI.start_date.substring(0,4), this.QI.start_date.substring(5,7)-1, this.QI.start_date.substring(8,10), this.QI.start_date.substring(11,13),this.QI.start_date.substring(14,16),this.QI.start_date.substring(17,19),this.QI.start_date.substring(20,23));
					}

					//end date.
					this.QI.end_date = i2b2.h.getXNodeVal(qiNode, 'end_date');
					if (!Object.isUndefined(this.QI.end_date)) {
						this.QI.end_date =  new Date(this.QI.end_date.substring(0,4), this.QI.end_date.substring(5,7)-1, this.QI.end_date.substring(8,10), this.QI.end_date.substring(11,13),this.QI.end_date.substring(14,16),this.QI.end_date.substring(17,19),this.QI.end_date.substring(20,23));
					}

					// found the query instance, extract the info
					this.QI.status = i2b2.h.XPath(qiNode, 'descendant-or-self::query_status_type/name')[0].firstChild.nodeValue;
					this.QI.statusID = i2b2.h.XPath(qiNode, 'descendant-or-self::query_status_type/status_type_id')[0].firstChild.nodeValue;
				}

				//add the compute time.
				self.dispDIV.innerHTML += '</div>';
				resultString += '\n'; //BG
				self.dispDIV.innerHTML += '<div style="margin-left:20px; clear:both; line-height:16px; ">Compute Time: '+ s +' secs</div>';
				resultString += 'Compute Time: '+ s + ' secs\n'; //BG
				
				// -- query result instance vars -- //
				var qriNodeList	= results.refXML.getElementsByTagName('query_result_instance'),
					qriIdx, qriNode, qriObj, breakdownType,
					errorObjects = [],
					brdNodeList, brdNode,  brdIdx, brdObj;

				//iterate through each query result.
				for (qriIdx = 0; qriIdx < qriNodeList.length; qriIdx++) {

					//init qri vars.
					qriNode         =  qriNodeList[qriIdx];
					qriObj          = parseQueryResultInstance(qriNode);
					breakdownType   = '';

					//which hospital
					self.dispDIV.innerHTML += '<div style="clear:both;"><br/><div style="float:left; font-weight:bold; margin-left:20px;">' + qriObj.description + ' "' +self.QM.name+ '"</div>';
					resultString += '\n'; //BG
					resultString += qriObj.description + ' "' + self.QM.name+ '"\n'; //BG

					//if there was an error display it.
					if((qriObj.statusName == "ERROR") || (qriObj.statusName == "UNAVAILABLE")){

						errorObjects.push(qriObj.problem);

						self.dispDIV.innerHTML += " &nbsp;- <span title='" + qriObj.statusDescription +"'>      <b><a class='query-error-anchor' href='#' style='color:#ff0000'>      <b><span color='#ff0000'>" + qriObj.problem.summary+ "</span></b></a></b></span>";
						resultString += 'ERROR : ' + qriObj.problem.summary + '\n'; //BG
						continue;
					}
					else if((qriObj.statusName == "PROCESSING")){
						self.dispDIV.innerHTML += " - <span><b><font color='#00dd00'>Still Processing Request</font></b></span>";
						resultString += 'Still Processing Request : Still Processing Request\n'; //BG
						continue;
					}
					else if(["COMPLETED","FINISHED"].indexOf(qriObj.statusName) < 0){
						self.dispDIV.innerHTML += " - <span><b><font color='#dd0000'>Results not available</font></b></span>";
						resultString += 'Results not available : Results not available\n'; //BG
						continue;
					}

					self.dispDIV.innerHTML += "<div style=\"clear: both; margin-left: 30px; float: left; height: 16px; line-height: 16px;\">"
						+ "Patient Count"
						+ ": <font color=\"#0000dd\">"
						+ i2b2.h.getFormattedResult(qriObj.setSize)
						+ "</font></div>";

					resultString += 'Patient Count' + i2b2.h.getFormattedResult(qriObj.setSize) + '\n'; //BG
					
					
					//grab breakdown data.
					brdNodeList = i2b2.h.XPath(qriNode, 'descendant-or-self::breakdown_data/column');

					for(brdIdx = 0; brdIdx < brdNodeList.length; brdIdx ++){

						//init brd vars.
						brdNode             = brdNodeList[brdIdx];
						brdObj              = parseBreakdown(brdNode);

						if(brdObj.parentResultType !== breakdownType){
							breakdownType = brdObj.parentResultType;
							self.dispDIV.innerHTML += "<div style=\"clear: both; margin-left: 30px; float: left; height: 16px; line-height: 16px;\"><br/><font>"
								+ getBreakdownTitle(brdObj.parentResultType)
								+ ":</font></div>";
							resultString += '\n'; //BG
							resultString += getBreakdownTitle(brdObj.parentResultType) + '\n'; //BG
							self.dispDIV.innerHTML += "<div style=\"clear: both; margin-left: 40px; float: left; height: 16px; line-height: 16px;\"></div>";
						}

						self.dispDIV.innerHTML += "<div style=\"clear: both; margin-left: 40px; float: left; height: 16px; line-height: 16px;\">"
							+ brdObj.name
							+ ": <font color=\"#0000dd\">"
							+ i2b2.h.getFormattedResult(brdObj.value)
							+ "</font></div>";
						resultString += brdObj.name + i2b2.h.getFormattedResult(brdObj.value) + '\n'; //BG
					}
				}
				i2b2.CRC.ctrlr.currentQueryResults = new i2b2.CRC.ctrlr.QueryResults(resultString);
				$hrine.EnhancedError.createErrorDialogue(self.dispDIV, errorObjects);
				i2b2.CRC.ctrlr.history.Refresh();
			}
			catch(err)
			{
				console.error(err);
			}
		}


        /**
         *
         * @param qriNode
         * @returns {{qiStatusName: string, qiStatusDescription: string, qiSetSize: string, qiDescription: string, qiResultName: string, qiResultDescription: string}}
         */
        function parseQueryResultInstance(qriNode){
            var qriObj = {
                statusName:           	grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/name'),
                statusDescription:    	grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/description'),
				description:			grabXmlNodeData(qriNode, 'descendant-or-self::description')
            };

            if(qriObj.statusName == "ERROR"){
				qriObj.problem = $hrine.EnhancedError.parseProblem(qriNode);
				return qriObj;
            }

            qriObj.setSize              =   grabXmlNodeData(qriNode, 'descendant-or-self::set_size');
            qriObj.resultName           =   grabXmlNodeData(qriNode, 'descendant-or-self::query_result_type/name');
            qriObj.resultDescription    =   grabXmlNodeData(qriNode, 'descendant-or-self::query_result_type/description');

            return qriObj;
        }

        /**
         *
         * @param brdNode
         */
        function parseBreakdown(brdNode){

            var brdObj = {
                name:               grabXmlNodeData(brdNode,    'name'),
                value:              grabXmlNodeData(brdNode,     'value'),
                parentResultType:   grabXmlNodeData(brdNode,  'parent::breakdown_data/resultType')
            }

            return brdObj;
        }


        /**
         * Return breakdown title based on breakdown type.
         * @param breakdownType
         * @returns {*}
         */
        function getBreakdownTitle(breakdownType) {


            return {
                'PATIENT_AGE_COUNT_XML':        'Patient Age Count Breakdown',
                'PATIENT_GENDER_COUNT_XML':     'Patient Gender Count Breakdown',
                'PATIENT_RACE_COUNT_XML':       'Patient Race Count Breakdown',
                'PATIENT_VITALSTATUS_COUNT_XML':'Patient Vital Status Count Breakdown'

            }[breakdownType];
        };

        /**
         * Grab data for node, return empty string if none.
         * @param node
         * @param xPathString
         * @returns {string}
         */
        function grabXmlNodeData(node, xPathString){
			var nodeVal = i2b2.h.XPath(node, xPathString);
            return (nodeVal.length)? nodeVal[0].firstChild.nodeValue : '';
        }

		// switch to status tab
		i2b2.CRC.view.status.showDisplay();

		// timer and display refresh stuff
		private_startTime = new Date();
		private_refreshInterrupt = setInterval("i2b2.CRC.ctrlr.currentQueryStatus.refreshStatus()", 100);

		// AJAX call
		i2b2.CRC.ajax.runQueryInstance_fromQueryDefinition("CRC:QueryTool", this.params, this.callbackQueryDef);
	}

	return {
		name: "",
		polling_interval: 1000,
		QM: {id:false, status:""},
		QI: {id:false, status:""},
		QRS:{},
		displayDIV: false,
		running: false,
		started: false,
		startQuery: function(queryName, ajaxParams) {
			this.name = queryName;
			this.params = ajaxParams;
			private_startQuery.call(this);
		},
		cancelQuery: function() {
			private_cancelQuery();
		},
		isQueryRunning: function() {
			return private_singleton_isRunning;
		},
		refreshStatus: function() {
			private_refresh_status();
		}
	};
}();

i2b2.CRC.ctrlr.currentQueryStatus = false; 

