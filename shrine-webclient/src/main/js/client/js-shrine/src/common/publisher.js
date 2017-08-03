//https://ilikekillnerds.com/2016/11/injection-inheritance-aurelia/
import {EventAggregator} from 'aurelia-event-aggregator';
import {commands} from 'common/shrine.messages';
export class Publisher{
    static inject = [EventAggregator, commands];
    constructor(evtAgg, commands){
        this.commands = commands;
        this.publish = (c,d) => evtAgg.publish(c,d);
    }
}