import {PubSub} from './pub-sub';
import {I2B2Service } from './i2b2.service';
export class I2B2PubSub extends PubSub{
    static inject = [I2B2Service];
    constructor(i2b2Svc, ...rest) {
        super(...rest)

        this.listen = () => {
            i2b2Svc.onResize((a, b) => b.find(e => e.action === 'ADD') ?
            () => this.publish(this.notifications.i2b2.tabMax) 
                : () => this.publish(this.notifications.i2b2.tabMin));
            i2b2Svc.onHistory(() => this.publish(this.notifications.i2b2.historyRefreshed));
            i2b2Svc.onQuery((e, d) => this.publish(this.notifications.i2b2.queryStarted, d[0].name));
            i2b2Svc.onNetworkId((e, d) => this.publish(this.notifications.i2b2.networkIdReceived, d[0]));
            i2b2Svc.onViewSelected(e => this.publish(this.notifications.i2b2.viewSelected, e.data));
            i2b2Svc.onExport(() => this.publish(this.notifications.i2b2.exportQuery)); 
            i2b2Svc.onClearQuery(() => this.publish(this.notifications.i2b2.clearQuery));         
            this.subscribe(this.commands.i2b2.cloneQuery, d => i2b2Svc.loadQuery(d));
            this.subscribe(this.commands.i2b2.showError, d => {
                i2b2Svc.errorDetail(d)
            });
            this.subscribe(this.commands.i2b2.renameQuery, d => i2b2Svc.renameQuery(d));
            this.subscribe(this.commands.i2b2.flagQuery, d => i2b2Svc.flagQuery(d));
            this.subscribe(this.commands.i2b2.unflagQuery, d => i2b2Svc.unflagQuery(d));
            this.subscribe(this.notifications.shrine.queryUnavailable, () => i2b2Svc.publishQueryUnavailable());
            this.subscribe(this.notifications.shrine.queryAvailable, () => i2b2Svc.publishQueryAvailable());
            this.subscribe(this.notifications.shrine.refreshAllHistory, () => i2b2Svc.loadHistory());
        }
    }
}


