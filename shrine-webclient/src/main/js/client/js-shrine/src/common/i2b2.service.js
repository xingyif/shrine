
import * as _ from 'ramda';
import {Container} from './container';

export class I2B2Service {
    constructor(context = window) {

        //private
        const ctx = Container.of(context);
        const applyIfExists = _.curry((d, f, c) => c.hasNothing() ? d : f(c)) (Container.of(null));
        const map = _.curry((el, v) => v.map(_.prop(el)));
        const prop = _.curry((el, c) => applyIfExists(map(el) , c));
        const i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
        const crc = _.compose(prop('CRC'), i2b2);
        const events = _.compose(prop('events'), i2b2);
       
        // -- @todo: makes assumption that i2b2 object conforms to predictable structure?  -- //
        this.onResize =  f => applyIfExists((c) => c.value.changedZoomWindows.subscribe(f), events(ctx));
        this.onHistory = f => applyIfExists((c) => c.value.ctrlr.history.events.onDataUpdate.subscribe(f), crc(ctx)); 
        this.onQuery = f => applyIfExists((c) => c.value.afterQueryInit.subscribe(f), events(ctx));
        this.loadHistory = () => applyIfExists((c) => c.value.view.history.doRefreshAll(), crc(ctx));
        this.loadQuery = id => applyIfExists((c) => c.value.ctrlr.QT.doQueryLoad(id), crc(ctx));
    }
}

