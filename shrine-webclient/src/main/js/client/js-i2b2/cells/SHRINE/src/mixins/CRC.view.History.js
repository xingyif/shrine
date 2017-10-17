export const contextMenuValidateMixin = context => {
  const ContextMenuValidate = context.i2b2.CRC.view.history.ContextMenuValidate;
  const ITEM_TEXT = 'Display'; 
  const needsDisplayQuery = items => items.filter(i => i.text === ITEM_TEXT).length === 0;
  return event => {
    const itemData = context.i2b2.CRC.view.history.ContextMenu.itemData;
    if(needsDisplayQuery(context.i2b2.CRC.view.history.ContextMenu.itemData)){
      itemData.unshift({ 
        text: "Display", 
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
    ContextMenuValidate(event);
  };
}
