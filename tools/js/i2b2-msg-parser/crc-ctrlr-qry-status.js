
// --- move to base object ? --- //
var i2b2;
var window;
var document;

function seedDOM(i2b2Window) {
    window = i2b2Window;
    i2b2 = window.i2b2;
    document = window.document;
}


// -- testing the parsing logic within the scoped callback nested in private_startQuery in CRC_ctrlr_QryStatus.js -- //
function refreshStatusCallback(results) {

     // -- simulate i2b2 -- //
    var self = {
        dispDIV: document.getElementById('infoQueryStatusText'),
        QM: {
            name: 'QM name'
        }
    };
    var QI = {};

    //if error
    if (results.error) {
        var temp = results.refXML.getElementsByTagName('response_header')[0];
        if (undefined != temp) {
            results.errorMsg = i2b2.h.XPath(temp, 'descendant-or-self::result_status/status')[0].firstChild.nodeValue;
            if (results.errorMsg.substring(0, 9) == 'LOCKEDOUT') {
                results.errorMsg = 'As an "obfuscated user" you have exceeded the allowed query repeat and are now LOCKED OUT, please notify your system administrator.';
            }
        }
        alert(results.errorMsg);
        //private_cancelQuery();
        return;
    }


    // -- query was successful -- //

    // this private function refreshes the display DIV
    var d = new Date();
    var t = Math.floor((d.getTime() - 0) / 100) / 10;
    var s = t.toString();
    if (s.indexOf('.') < 0) {
        s += '.0';
    }

    self.dispDIV.innerHTML = '<div style="clear:both;"><div style="float:left; font-weight:bold">Finished Query: "' + self.QM.name + '"</div>';
    self.dispDIV.innerHTML += '<div style="float:right">[' + s + ' secs]</div><br/>';
    var resultString = 'Finished Query: "' + self.QM.name + '"\n';  //BG
    resultString += '[' + s + ' secs]\n'; //BG

    var runBoxText = document.getElementById('runBoxText');
    runBoxText.innerHTML = 'Run Query';

    //------------ QI Logic -------------------//

    //BG find user id
    var temp = results.refXML.getElementsByTagName('query_master')[0];

    //Update the userid element when query is run first time
    var userId = i2b2.h.getXNodeVal(temp, 'user_id', document);
    var userIdElem;
    if (userId) {
        userIdElem = document.getElementById('userIdElem');
        userIdElem.value = userId;
    }


    // -- todo: break into QueryInstance Parse method -- //
    // find our query instance
    var qi_list = results.refXML.getElementsByTagName('query_instance');  //Original code commented by BG
    var l = qi_list.length;

    for (var i = 0; i < l; i++) {
        var qiNode = qi_list[i];
        var qi_id = i2b2.h.xPath(qiNode, 'descendant-or-self::query_instance_id', document)[0].firstChild.nodeValue;
        QI.message = i2b2.h.getXNodeVal(qiNode, 'message', document);

        //start date.
        QI.start_date = i2b2.h.getXNodeVal(qiNode, 'start_date', document);
        if (QI.start_date) {

            // -- todo: move to method -- //
            var year = QI.start_date.substring(0, 4)
            var month = QI.start_date.substring(5, 7) - 1;
            var date = QI.start_date.substring(8, 10);
            var hour = QI.start_date.substring(11, 13);
            var minutes = QI.start_date.substring(14, 16);
            var seconds = QI.start_date.substring(17, 19);
            var miliseconds = QI.start_date.substring(20, 23)
            QI.start_date = new Date(year, month, date, hour, minutes, seconds, miliseconds);
        }


        //end date.
        QI.end_date = i2b2.h.getXNodeVal(qiNode, 'end_date', document);
        if (QI.end_date) {
            var year = QI.end_date.substring(0, 4)
            var month = QI.end_date.substring(5, 7) - 1;
            var date = QI.end_date.substring(8, 10);
            var hour = QI.end_date.substring(11, 13);
            var minutes = QI.end_date.substring(14, 16);
            var seconds = QI.end_date.substring(17, 19);
            var miliseconds = QI.end_date.substring(20, 23)
            QI.end_date = new Date(year, month, date, hour, minutes, seconds, miliseconds);
        }

        // found the query instance, extract the info
        QI.status = i2b2.h.xPath(qiNode, 'descendant-or-self::query_status_type/name', document)[0].firstChild.nodeValue;
        QI.statusID = i2b2.h.xPath(qiNode, 'descendant-or-self::query_status_type/status_type_id', document)[0].firstChild.nodeValue;
    }


    //add the compute time.
    self.dispDIV.innerHTML += '</div>';
    resultString += '\n'; //BG
    self.dispDIV.innerHTML += '<div style="margin-left:20px; clear:both; line-height:16px; ">Compute Time: ' + s + ' secs</div>';
    resultString += 'Compute Time: ' + s + ' secs\n'; //BG

    // -- query result instance vars -- //
    var qriNodeList = results.refXML.getElementsByTagName('query_result_instance'),
        qriIdx, qriNode, qriObj, breakdownType,
        errorObjects = [],
        brdNodeList, brdNode, brdIdx, brdObj;

    //iterate through each query result.
    for (qriIdx = 0; qriIdx < qriNodeList.length; qriIdx++) {

        //init qri vars.
        qriNode = qriNodeList[qriIdx];
        qriObj = parseQueryResultInstance(qriNode, document);
        breakdownType = '';

        //which hospital
        self.dispDIV.innerHTML += '<div style="clear:both;"><br/><div style="float:left; font-weight:bold; margin-left:20px;">' + qriObj.description + ' "' + self.QM.name + '"</div>';
        resultString += '\n'; //BG
        resultString += qriObj.description + ' "' + self.QM.name + '"\n'; //BG
    }

}


/**
 *
 * @param qriNode
 * @returns {{qiStatusName: string, qiStatusDescription: string, qiSetSize: string, qiDescription: string, qiResultName: string, qiResultDescription: string}}
 */
function parseQueryResultInstance(qriNode, document) {

    var $hrine = window.$hrine;

    var qriObj = {
        statusName: grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/name', document),
        statusDescription: grabXmlNodeData(qriNode, 'descendant-or-self::query_status_type/description', document),
        description: grabXmlNodeData(qriNode, 'descendant-or-self::description', document)
    };

    if (qriObj.statusName == "ERROR") {
        qriObj.problem = $hrine.EnhancedError.parseProblem(qriNode);
        return qriObj;
    }

    qriObj.setSize = grabXmlNodeData(qriNode, 'descendant-or-self::set_size', document);
    qriObj.resultName = grabXmlNodeData(qriNode, 'descendant-or-self::query_result_type/name', document);
    qriObj.resultDescription = grabXmlNodeData(qriNode, 'descendant-or-self::query_result_type/description', document);

    return qriObj;
}

/**
 *
 * @param brdNode
 */
function parseBreakdown(brdNode) {

    var brdObj = {
        name: grabXmlNodeData(brdNode, 'name'),
        value: grabXmlNodeData(brdNode, 'value'),
        parentResultType: grabXmlNodeData(brdNode, 'parent::breakdown_data/resultType')
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
        'PATIENT_AGE_COUNT_XML': 'Patient Age Count Breakdown',
        'PATIENT_GENDER_COUNT_XML': 'Patient Gender Count Breakdown',
        'PATIENT_RACE_COUNT_XML': 'Patient Race Count Breakdown',
        'PATIENT_VITALSTATUS_COUNT_XML': 'Patient Vital Status Count Breakdown'

    }[breakdownType];
};

/**
 * Method for hiding the precise value of a query below a certain result.
 * @param resultCount 			- the number of results from a query.
 * @param obfuscationSetting 	- do not reveal this number of results
 */
function getObfuscatedResult(resultCount, obfuscationSetting) {

    var resultTitle = " - ",
        name = " patients",
        offsetText = " +-3",
        isException = i2b2.PM.model.isObfuscated === false;

    //default to 10.
    obfuscationSetting = (arguments.length > 1) ? arguments[1] : 10;

    //if user role is an exception.  return result.
    if (isException) {
        return resultTitle += resultCount + name;
    }

    resultTitle += ((resultCount >= obfuscationSetting) ?
        resultCount + offsetText + name : obfuscationSetting + name + " or fewer");

    return resultTitle;
}


/**
 * Grab data for node, return empty string if none.
 * @param node
 * @param xPathString
 * @returns {string}
 */
function grabXmlNodeData(node, xPathString, document) {
    var nodeVal = i2b2.h.xPath(node, xPathString, document);
    return (nodeVal.length) ? nodeVal[0].firstChild.nodeValue : '';
}

// -- node singleton -- //
module.exports = {
    seedDOM: seedDOM,
    refreshStatusCallback: refreshStatusCallback
}