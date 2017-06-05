import { inject } from 'aurelia-framework';
import { I2B2Service } from './i2b2.service';
import { TabsModel } from './tabs.model';

@inject(I2B2Service, TabsModel)
export class I2B2PubSub {
    constructor(i2b2Svc, tabs) {
        this.listen = () => {
            const setVertStyle = (a, b) => b.find(e => e.action === 'ADD') ? tabs.setMax() : tabs.setMin();
            i2b2Svc.onResize(setVertStyle);
        }
    }
}


