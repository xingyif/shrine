import {customElement, bindable} from 'aurelia-framework';
//file:///Users/ben/Downloads/SHRINETEAM-46892567-020817-1500-358.pdf
@customElement('node-result')
export class NodeResult{
    @bindable result;
    @bindable queryName;
}