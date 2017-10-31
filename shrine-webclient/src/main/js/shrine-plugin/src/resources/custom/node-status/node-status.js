import {customElement, bindable} from 'aurelia-framework';
import {PubSub} from 'services/pub-sub';
@customElement('node-status')
export class NodeStatus extends PubSub{
    @bindable result
    constructor(...rest) {
        super(...rest);
    }
}