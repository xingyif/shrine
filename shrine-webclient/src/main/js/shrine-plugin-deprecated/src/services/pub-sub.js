//https://ilikekillnerds.com/2016/11/injection-inheritance-aurelia/
import {EventAggregator} from 'aurelia-event-aggregator';
import {commands, notifications} from './shrine.messages';
export class PubSub{
    static inject = [EventAggregator, commands, notifications];
    constructor(evtAgg, commands, notifications){
        this.commands = commands;
        this.notifications = notifications;
        this.publish = (c,d) => evtAgg.publish(c,d);
        this.subscribe = (n, fn) => evtAgg.subscribe(n, fn);
    }
}