
import * as _ from 'ramda';
import {Container} from './container';

export class I2B2Service {
    constructor(context = window) {

        //private
        const ctx = Container.of(context);
        const nullOrSomething = _.curry((d, f, c) => c.hasNothing() ? d : f(c)) (Container.of(null));
        const prop = _.curry((el, c) => nullOrSomething((v) => v.map(_.prop(el)), c));
        const i2b2 = _.compose(prop('i2b2'), prop('window'), prop('parent'));
        const crc = _.compose(prop('CRC'), i2b2);
        const events = _.compose(prop('events'), i2b2);
        
        // -- @todo: makes assumption that i2b2 object conforms to predictable structure?  -- //
        this.onResize =  f => nullOrSomething((c) => c.value.changedZoomWindows.subscribe(f), events(ctx));
        this.onHistory = f => nullOrSomething((c) => c.value.ctrlr.history.events.onDataUpdate.subscribe(f), crc(ctx)); 
        this.loadHistory = () => nullOrSomething((c) => c.value.view.history.doRefreshAll(), crc(ctx));
        this.loadQuery = id => nullOrSomething((c) => c.value.ctrlr.QT.doQueryLoad(id), crc(ctx));
    }
}

