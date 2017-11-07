const getChildRecordsMixin = context => (sdxParentNode, onCompleteCallback) => {
  const {origData:{id:networkId}} = sdxParentNode;
  context.i2b2.CRC.ctrlr.QT.doQueryLoad(networkId);
}

export default getChildRecordsMixin;