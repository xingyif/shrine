import { bindable, inject } from 'aurelia-framework';

@inject(Element)
export class Paginator {
    @bindable pages;
    constructor(element) {
        Paginator.prototype.init = () => {
            this.index = 0;
            this.element = element;
        }
        this.init();
    }

    set pageIndex(i) {
        const max = this.pages.length - 1;
        this.index = i < 0 ? 0 : (i > max ? max : i);
        this.element.dispatchEvent(new CustomEvent('paginator-click', {
            detail: {index: this.index},
            bubbles: true,
            cancelable: true
        }));
    }
}