export class I2B2Service {
    constructor(context = window) {
        const i2b2 = getLib(context, 'i2b2');
        this.onResize = f => (i2b2) ? i2b2.events.changedZoomWindows.subscribe(f) : null;
        this.onHistory = (i2b2)? i2b2.CRC.view.history.doRefreshAll  : null;
        this.loadQuery = id => (i2b2)? i2b2.CRC.ctrlr.QT.doQueryLoad(id) :  null;
    }
}

//@todo compose these functions?
const getLib = (context, lib) => hasParent(context) ? getParent(context)[lib] : null;
const getParent = (context) => context.parent.window;
const hasParent = (context) => context && context.parent && context.parent.window;
