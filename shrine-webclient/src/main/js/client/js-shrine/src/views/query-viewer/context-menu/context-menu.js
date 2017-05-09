import {inject, bindable} from 'aurelia-framework';
import { I2B2Service } from 'common/i2b2.service.js';

@inject(I2B2Service)
export class ContextMenu {
    @bindable context;

    constructor(i2b2Svc) {
        this.loadQuery = id => {
            i2b2Svc.loadQuery(id);
            this.context.class = 'hide';
        }

      this.loadHistory = () => {
        this.context.class = 'hide';
        i2b2Svc.onHistory();
      };
    }
}