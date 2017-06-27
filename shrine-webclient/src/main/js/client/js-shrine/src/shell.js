import { inject } from 'aurelia-framework';
import {EventAggregator} from 'aurelia-event-aggregator';
import { I2B2PubSub } from 'common/i2b2.pub-sub';
import { QueriesModel } from 'common/queries.model';
import {notifications} from 'common/shrine.messages';
@inject(EventAggregator, I2B2PubSub, QueriesModel, notifications)
export class Shell {

  constructor(evtAgg, i2b2PubSub, queries, notifications) {

    Shell.prototype.init = () => {
      this.view = 'query-viewer';
    }
    this.init();

    i2b2PubSub.listen();
    queries.load();
    evtAgg.subscribe(notifications.i2b2.viewSelected, v => {
        this.view = v;
    });
  }
}
