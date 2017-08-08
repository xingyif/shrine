import {customElement, bindable} from 'aurelia-framework';
import {Publisher} from 'common/publisher';
@customElement('node-status')
export class NodeStatus extends Publisher{
    @bindable result
    constructor(...rest) {
        super(...rest);
    }
}