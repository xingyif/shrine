import {inject, bindable} from 'aurelia-framework';
import {EventAggregator} from 'aurelia-event-aggregator';
import {commands} from 'common/shrine.messages'
//import { I2B2Service } from 'common/i2b2.service.js';

@inject(EventAggregator, commands)
export class ContextMenu {
    @bindable context;

    constructor(evtAgg, commands) {
        ContextMenu.prototype.cloneQuery = id => {
            evtAgg.publish(commands.i2b2.cloneQuery, id);
            this.context.class = 'hide';
        }

        ContextMenu.prototype.refreshHistory = () => {
            evtAgg.publish(commands.i2b2.refreshHistory);
            this.context.class = 'hide';
        }      
    }
}