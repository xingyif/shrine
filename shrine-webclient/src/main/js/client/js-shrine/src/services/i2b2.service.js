
import * as _ from 'ramda';
import {Container} from './container';
export class I2B2Service {
    static inject = [window];
    constructor(context) {

        //private
        const ctx = context? Container.of(context) : Container.of(null);
        const prop = _.curry((m, c) => c.value? Container.of(_.prop(m, c.value)) : Container.of(null));
        const i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
        const crc = _.compose(prop('CRC'), i2b2);
        const events = _.compose(prop('events'), i2b2);
        const shrine = _.compose(prop('SHRINE'), i2b2);
       
        // -- @todo: makes assumption that i2b2 object conforms to predictable structure?  -- //
        I2B2Service.prototype.onResize =  f => events(ctx).map((v) => v.changedZoomWindows.subscribe(f));
        I2B2Service.prototype.onHistory = f => crc(ctx).map((v) => v.ctrlr.history.events.onDataUpdate.subscribe(f)); 
        I2B2Service.prototype.onQuery = f => events(ctx).map((v) => v.afterQueryInit.subscribe(f));
        I2B2Service.prototype.onNetworkId = f => events(ctx).map(v => v.networkIdReceived.subscribe(f)); 
        I2B2Service.prototype.onViewSelected = f => prop('addEventListener', ctx).value? 
            Container.of(ctx.value.addEventListener('message', f, false)) : Container.of(null); 
        I2B2Service.prototype.onExport = f => events(ctx).map(v => v.exportQueryResult.subscribe(f));
        I2B2Service.prototype.onClearQuery = f => events(ctx).map(v => v.clearQuery.subscribe(f));
        I2B2Service.prototype.loadHistory = () => crc(ctx).map((v) => v.view.history.doRefreshAll());
        I2B2Service.prototype.loadQuery = id => crc(ctx).map((v) => v.ctrlr.QT.doQueryLoad(id));
        I2B2Service.prototype.errorDetail = d => shrine(ctx).map((v) => {
            v.plugin.errorDetail(d)
        });
        I2B2Service.prototype.renameQuery = id => crc(ctx).map(v => v.ctrlr.history.queryRename(id, false));
        I2B2Service.prototype.flagQuery = id => crc(ctx).map(v => v.ctrlr.history.Flag({ queryId: id, message: ''}));
        I2B2Service.prototype.unflagQuery = id => crc(ctx).map(v => v.ctrlr.history.Unflag({ queryId: id}));
        I2B2Service.prototype.publishQueryUnavailable = () => events(ctx).map(v => v.queryResultUnavailable.fire());
        I2B2Service.prototype.publishQueryAvailable = () => events(ctx).map(v => v.queryResultAvailable.fire()); 
        I2B2Service.prototype.publishRefreshAllHistory = () => events(ctx).map(v => v.refreshAllHistory.fire());    
    }
}

