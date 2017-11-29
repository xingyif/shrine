const getChildRecordsMixin = context => (sdxParentNode, onCompleteCallback) => {
  //TODO: destructure this!!!
  const networkId = sdxParentNode.origData.id
  const name = sdxParentNode.origData.realname;
  context.i2b2.events.clearQuery.fire();
  context.i2b2.events.networkIdReceived.fire({networkId, name});
  context.i2b2.CRC.ctrlr.QT.doQueryLoad(networkId);
  context.i2b2.CRC.view.history.yuiTree.locked = false;
  context.i2b2.CRC.view.history.yuiTree._nodes.map(n => n.isLoading = false);
}

export default getChildRecordsMixin;