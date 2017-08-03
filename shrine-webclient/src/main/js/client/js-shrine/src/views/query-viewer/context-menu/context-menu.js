import {bindable} from 'aurelia-framework';
import {EventAggregator} from 'aurelia-event-aggregator';
import {commands} from 'common/shrine.messages'

export class ContextMenu {
    @bindable context;
    static inject = [EventAggregator, commands];
    constructor(evtAgg, commands) {
        ContextMenu.prototype.cloneQuery = id => {
            evtAgg.publish(commands.i2b2.cloneQuery, id);
            this.context.class = 'hide';
        }
        ContextMenu.prototype.renameQuery =  id => {
            evtAgg.publish(commands.i2b2.renameQuery, id);
            this.context.class = 'hide';
        }
        ContextMenu.prototype.flagQuery =  id => {
            evtAgg.publish(commands.i2b2.flagQuery, id);
            this.context.class = 'hide';
        }

        ContextMenu.prototype.unflagQuery =  id => {
            evtAgg.publish(commands.i2b2.unflagQuery, id);
            this.context.class = 'hide';
        }    
    }
}