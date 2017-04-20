export class I2B2Service {
    constructor(context = window) {
        const i2b2 = getLib(context, 'i2b2');
        this.onResize = f => (i2b2) ? i2b2.events.changedZoomWindows.subscribe(f) : null;
        this.onHistory = f => (i2b2)? i2b2.CRC.ctrlr.history.events.onDataUpdate.subscribe(f) : null;
    }
}
const getLib = (context, lib) => hasParent(context) ? getParent(context)[lib] : null;
const getParent = (context) => context.parent.window;
const hasParent = (context) => context && context.parent && context.parent.window;
