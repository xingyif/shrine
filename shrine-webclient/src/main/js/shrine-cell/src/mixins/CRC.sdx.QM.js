const getChildRecordsMixin = context => (sdxParentNode, onCompleteCallback) => {
  const {origData:{id:networkId,name}} = sdxParentNode;
  //TODO: fix destructuring logic above!!!
  context.i2b2.events.networkIdReceived.fire({networkId, name: cr.origData.name});
  context.i2b2.CRC.ctrlr.QT.doQueryLoad(cr.origData.id);
  context.i2b2.CRC.view.history.yuiTree.locked = false;
  context.i2b2.CRC.view.history.yuiTree._nodes.map(n => n.isLoading = false);
}

export default getChildRecordsMixin;