const queryRun = (context) =>
  (inQueryName, options) => {

    context.shrine.plugin.disableRunQueryButton();
    context.i2b2.events.afterQueryInit.fire({ name: inQueryName, data: options });
    
    // make sure name is not blank
    if (inQueryName.blank()) {
      alert('Cannot run query with without providing a name!');
      return;
    }

    // Query Parameters
    //this._geQueryXMl is referenced...can reference it globally.
    var query_definition = context.i2b2.CRC.ctrlr.QT._getQueryXML(inQueryName);
    
    var params = {
      result_wait_time: context.i2b2.CRC.view.QT.params.queryTimeout,
      psm_query_definition: query_definition.queryXML
    }
    // SHRINE topic if we are running SHRINE query
    if (context.i2b2.h.isSHRINE()) {
      var topicSELECT = context.prototype$('queryTopicSelect');
      if (topicSELECT) {
        if (topicSELECT.selectedIndex == null || topicSELECT.selectedIndex == 0) {
          alert("Please select a Topic to run the query.");
          return false;
        }
        params.shrine_topic = "<shrine><queryTopicID>" + topicSELECT.options[topicSELECT.selectedIndex].value + "</queryTopicID></shrine>";
      }
    }

    // generate the result_output_list (for 1.3 backend)

    var result_output = "";
    for (var name in options) {
      if (name) {
        i++;
        result_output += '<result_output priority_index="' + i + '" name="' + name.substring(4).toLowerCase() + '"/>\n';
      }

    }

    //@pcori_webclient
    //result_output = '<result_output priority_index="11" name="patient_count_xml"/>';
    params.psm_result_output = '<result_output_list>' + result_output + '</result_output_list>\n';

    // create query object
    context.i2b2.CRC.ctrlr.currentQueryStatus = new context.i2b2.CRC.ctrlr.QueryStatus(context.prototype$('infoQueryStatusText'));
    //override query callback here.
    context.i2b2.CRC.ctrlr.currentQueryStatus.startQuery(inQueryName, params);
  }

  export default queryRun;