import {customElement, bindable} from 'aurelia-framework';
@customElement('node-result')
export class NodeResult{
    @bindable result;
    @bindable queryName;
}