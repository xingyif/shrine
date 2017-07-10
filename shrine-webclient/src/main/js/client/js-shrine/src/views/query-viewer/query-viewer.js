import { inject } from 'aurelia-framework';
import { EventAggregator } from 'aurelia-event-aggregator'
import { QueriesModel } from 'common/queries.model'
import { ScrollService } from './scroll.service';
import { notifications, commands } from 'common/shrine.messages';

@inject(EventAggregator, QueriesModel, notifications, commands)
export class QueryViewer {
    constructor(evtAgg, queries, notifications, commands) {

        QueryViewer.prototype.init = () => {
            this.pageIndex = 0;
            this.showLoader = true;
            this.vertStyle = 'v-min';
            this.runningQueryName = null;
        }
        this.init();

        QueryViewer.prototype.setToPage = i => {
            this.pageIndex = i;
            this.page = this.pages[this.pageIndex];
        }
        const scrolledToBottom =
            e => ScrollService.scrollRatio(e).value === 1;
        QueryViewer.prototype.onScroll = e => {
            if (scrolledToBottom(e) && !this.loadingInfiniteScroll && queries.moreToLoad()) {
                this.loadingInfiniteScroll = true;
                queries.load();
            }
        }

        QueryViewer.prototype.publishError = (e, r) => { 
            e.stopPropagation();
            return evtAgg.publish(commands.i2b2.showError, r);
        }
        QueryViewer.prototype.getContext = (e, r, c) => 
            ({ x: e.pageX, y: e.pageY, class: 'show', query: r, isCount: c !== undefined, count: c});
     

        //notifications @todo:  remove subscriptions on detach?
        evtAgg.subscribe(notifications.i2b2.historyRefreshed, () => queries.load());
        evtAgg.subscribe(notifications.i2b2.tabMax, () => this.vertStyle = 'v-full');
        evtAgg.subscribe(notifications.i2b2.tabMin, () => this.vertStyle = 'v-min');
        evtAgg.subscribe(notifications.i2b2.queryStarted, n => this.runningQueryName = n);
        evtAgg.subscribe(notifications.shrine.queriesReceived, d => {
            this.pages = d;
            this.page = this.pages[0];
            this.runningQueryName = null;
            this.loadingInfiniteScroll = false;
            this.showLoader = false;
        });
    }
    updatePage($event) {
        $event.stopPropagation();
        const index = event.detail.index;
        this.page = this.pages[index];
    }
}



