const getChildRecordsMixin = context => (sdxParentNode, onCompleteCallback) => {
  const {origData:{id:networkId}} = sdxParentNode;
  context.i2b2.CRC.ctrlr.QT.doQueryLoad(networkId);
  context.i2b2.CRC.view.history.yuiTree.locked = false;
  //context.i2b2.CRC.view.history.doRefreshAll(); //TODO:  trying this out.
  //in CRC_view_History.js:
  //see i2b2.CRC.view.history.PopulateQueryMasters = function(dm_ptr, dm_name, options) { 
  //TODO:  what about re-loading history?
  /*context.i2b2.CRC.view.history.yuiTree.nodes.map(n => {
    n.locked = false;
  });*/

  //OR
  //context.i2b2.CRC..view.history.doRefreshAll());
}

export default getChildRecordsMixin;