
import * as _ from 'ramda';
import {Container} from './container';

export class I2B2Service {
    constructor(context = window) {

        //private
        const ctx = Container.of(context);
        const prop = _.curry((el, c) => c.value? Container.of(_.prop(el, c.value)) : Container.of(null));
        const i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
        const crc = _.compose(prop('CRC'), i2b2);
        const events = _.compose(prop('events'), i2b2);
       
        // -- @todo: makes assumption that i2b2 object conforms to predictable structure?  -- //
        this.onResize =  f => events(ctx).map((v) => v.changedZoomWindows.subscribe(f));
        this.onHistory = f => crc(ctx).map((v) => v.ctrlr.history.events.onDataUpdate.subscribe(f)); 
        this.onQuery = f => events(ctx).map((v) => v.afterQueryInit.subscribe(f)); 
        this.loadHistory = () => crc(ctx).map((v) => v.view.history.doRefreshAll());
        this.loadQuery = id => crc(ctx).map((v) => v.ctrlr.QT.doQueryLoad(id));
    }
}

