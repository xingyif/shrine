import jQuery from 'jquery';
class ShrineDom {
  static hideI2B2Tabs = () =>  {
    jQuery('#crcStatusBox .TopTabs').find('.tabBox').hide();
    return ShrineDom;
  }
  static hideI2B2Panels = () => {
    jQuery('#crcStatusBox .StatusBox').children().hide();
    return ShrineDom;
  }
  static removeI2B2PrintIcon = () => {
    jQuery('#crcStatusBox .TopTabs .opXML').children().first().remove();
    return ShrineDom
  }
  static removeI2B2PrintQueryBox = () => {
    jQuery("#printQueryBox").remove();
    return ShrineDom;
  }
  static addExportIcon = markup => {
    jQuery('#crcStatusBox .TopTabs .opXML').prepend(markup);
    return ShrineDom;
  }
  static addShrineTab = markup =>  {
    jQuery('#crcStatusBox .TopTabs').append(markup);
    return ShrineDom;
  }
  static addShrinePanel = markup => {
    jQuery('#crcStatusBox .StatusBox').append(markup);
    return ShrineDom;
  }
  static shrineCSVExport = () => {
    return jQuery('#crcStatusBox .TopTabs .opXML #shrineCSVExport');
  }
}

export default ShrineDom;