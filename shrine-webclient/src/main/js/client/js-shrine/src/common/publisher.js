//https://ilikekillnerds.com/2016/11/injection-inheritance-aurelia/
import {inject} from 'aurelia-framework';
import {EventAggregator} from 'aurelia-event-aggregator';
import {commands} from 'common/shrine.messages';
@inject(EventAggregator, commands)
export class Publisher{
    constructor(evtAgg, commands){
        this.commands = commands;
        this.publish = (c,d) => evtAgg.publish(c,d);
    }
}