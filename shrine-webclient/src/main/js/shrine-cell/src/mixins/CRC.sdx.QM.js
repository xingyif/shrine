const getChildRecordsMixin = context => (sdxParentNode, onCompleteCallback) => {
  const {origData:{id:networkId}} = sdxParentNode;
  context.i2b2.CRC.ctrlr.QT.doQueryLoad(networkId);
  context.i2b2.CRC.view.history.yuiTree.locked = false;
}

export default getChildRecordsMixin;