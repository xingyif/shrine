
import * as _ from 'ramda';
import {Container} from './container';

export class I2B2Service {
    constructor(context = window) {

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
        I2B2Service.prototype.onViewSelected = f => prop('addEventListener', ctx).value? 
            Container.of(ctx.value.addEventListener('message', f, false)) : Container.of(null); 

        // commands
        I2B2Service.prototype.loadHistory = () => crc(ctx).map((v) => v.view.history.doRefreshAll());
        I2B2Service.prototype.loadQuery = id => crc(ctx).map((v) => v.ctrlr.QT.doQueryLoad(id));
        I2B2Service.prototype.errorDetail = d => shrine(ctx).map((v) => v.plugin.errorDetail(d));
        
    }
}

