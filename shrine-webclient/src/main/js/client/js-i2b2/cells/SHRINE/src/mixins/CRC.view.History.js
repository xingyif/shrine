export const contextMenuValidateMixin = context => {
  const ContextMenuValidate = context.i2b2.CRC.view.history.ContextMenuValidate;
  const ITEM_TEXT = 'Display';
  const needsDisplayQuery = items => items.filter(i => i.text === ITEM_TEXT).length === 0;

  return function (p_oEvent) {
    const itemData = context.i2b2.CRC.view.history.ContextMenu.itemData;
    if(needsDisplayQuery(itemData)){
      itemData.unshift({ 
        text: ITEM_TEXT, 
        onClick: {
          fn: () => {
            const cr = context.i2b2.CRC.view.history.contextRecord;
            if(cr && cr.origData){
                context.i2b2.events.networkIdReceived.fire({networkId: cr.origData.id, name: cr.origData.name});
                context.i2b2.CRC.ctrlr.QT.doQueryLoad(cr.origData.id);
            } 
          }
        }
      })
    }


    var clickId = null;
    var currentNode = this.contextEventTarget;
    while (!currentNode.id) {
      if (currentNode.parentNode) {
        currentNode = currentNode.parentNode;
      } else {
        // we have recursed up the tree to the window/document DOM... it's a bad click
        this.cancel();
        return;
      }
    }
    clickId = currentNode.id;
    // see if the ID maps back to a treenode with SDX data
    var tvNode = context.i2b2.CRC.view.history.yuiTree.getNodeByProperty('nodeid', clickId);
    if (tvNode) {
      if (tvNode.data.i2b2_SDX) {
        if (tvNode.data.i2b2_SDX.sdxInfo.sdxType == "QM") {
          context.i2b2.CRC.view.history.contextRecord = tvNode.data.i2b2_SDX;
        } else {
          this.cancel();
          return;
        }
      }
    }
  };
}
