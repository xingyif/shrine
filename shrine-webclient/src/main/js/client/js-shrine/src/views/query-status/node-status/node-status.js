import {bindable} from 'aurelia-framework';
import {Publisher} from 'common/publisher';
export class NodeStatus extends Publisher{
    @bindable result
    constructor(...rest) {
        super(...rest);
    }
}