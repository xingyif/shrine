
import {EventAggregator} from 'aurelia-event-aggregator'
import {I2B2Service } from './i2b2.service';
import {notifications, commands} from './shrine.messages';
export class I2B2PubSub {
    static inject = [EventAggregator, I2B2Service, notifications, commands];
    constructor(evtAgg, i2b2Svc, notifications, commands) {
        this.listen = () => {
            i2b2Svc.onResize((a, b) => b.find(e => e.action === 'ADD') ?
                notifyTabMax() : notifyTabMin());
            i2b2Svc.onHistory(() => notifyHistoryRefreshed());
            i2b2Svc.onQuery((e, d) => notifyQueryStarted(d[0].name));
            i2b2Svc.onNetworkId((e, d) => notifyNetworkIdReceived(d[0].networkId));
            i2b2Svc.onViewSelected(e => notifyViewSelected(e.data));
            evtAgg.subscribe(commands.i2b2.cloneQuery, commandCloneQuery);
            evtAgg.subscribe(commands.i2b2.showError, commandShowError);
            evtAgg.subscribe(commands.i2b2.renameQuery, commandRenameQuery);
            evtAgg.subscribe(commands.i2b2.flagQuery, commandFlagQuery);
            evtAgg.subscribe(commands.i2b2.unflagQuery, commandUnflagQuery);
        }

        // -- notifications-- //
        const notifyTabMax = () => evtAgg.publish(notifications.i2b2.tabMax);
        const notifyTabMin = () => evtAgg.publish(notifications.i2b2.tabMin);
        const notifyHistoryRefreshed = () => evtAgg.publish(notifications.i2b2.historyRefreshed);
        const notifyQueryStarted = n => evtAgg.publish(notifications.i2b2.queryStarted, n);
        const notifyViewSelected = v => evtAgg.publish(notifications.i2b2.viewSelected, v);
        const notifyNetworkIdReceived = v => evtAgg.publish(notifications.i2b2.networkIdReceived, v);

        // -- commands --//
        const commandCloneQuery = d => i2b2Svc.loadQuery(d);
        const commandShowError = d => {
            console.log(`${commands.i2b2.showError}:  ${d}`);
            i2b2Svc.errorDetail(d);
        }
        const commandRenameQuery = d => i2b2Svc.renameQuery(d);
        const commandFlagQuery = d => i2b2Svc.flagQuery(d);
        const commandUnflagQuery = d => i2b2Svc.unflagQuery(d);
    }
}


