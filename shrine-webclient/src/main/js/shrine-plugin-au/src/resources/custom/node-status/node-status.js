import {customElement, bindable} from 'aurelia-framework';
import {PubSub} from 'services/pub-sub';
@customElement('node-status')
@bindable('result')
export class NodeStatus extends PubSub {
    constructor(...rest) {
        super(...rest);
    }
}