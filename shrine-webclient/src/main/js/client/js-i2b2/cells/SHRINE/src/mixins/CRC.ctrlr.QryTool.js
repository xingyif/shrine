export const queryRunMixin = (context) => 
  (inQueryName, options) => {

    context.shrine.plugin.disableRunQueryButton();
    context.i2b2.events.afterQueryInit.fire({ name: inQueryName, data: options });
    
    // make sure name is not blank
    if (inQueryName.blank()) {
      alert('Cannot run query with without providing a name!');
      return;
    }

    // Query Parameters
    var query_definition = context.i2b2.CRC.ctrlr.QT._getQueryXML(inQueryName);
    
    var params = {
      result_wait_time: context.i2b2.CRC.view.QT.params.queryTimeout,
      psm_query_definition: query_definition.queryXML
    }

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

    var result_output = "";
    for (var name in options) {
      if (name) {
        i++;
        result_output += '<result_output priority_index="' + i + '" name="' + name.substring(4).toLowerCase() + '"/>\n';
      }
    }

    params.psm_result_output = '<result_output_list>' + result_output + '</result_output_list>\n';
    context.i2b2.CRC.ctrlr.currentQueryStatus = new context.i2b2.CRC.ctrlr.QueryStatus(context.prototype$('infoQueryStatusText'));
    context.i2b2.CRC.ctrlr.currentQueryStatus.startQuery(inQueryName, params);
  }


  export const queryClearMixin = (context) => {
    const doQueryClear = context.i2b2.CRC.ctrlr.QT.doQueryClear;
    clearStatus => {
      doQueryClear.apply(context.i2b2.CRC.ctrlr.QT, []);
      if (clearStatus === true) context.i2b2.events.clearQuery.fire();
    } 
  }


