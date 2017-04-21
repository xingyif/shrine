import { inject, computedFrom } from 'aurelia-framework';
import { QueryViewerService } from 'views/query-viewer/query-viewer.service';
import { I2B2Service } from 'common/i2b2.service.js';

@inject(QueryViewerService, I2B2Service)
export class QueryViewer {
    constructor(service, i2b2Svc) {

        // -- init -- //
        this.screenIndex = 0;
        this.showCircles = false;
        this.service = service;
        this.vertStyle = 'v-min';

        // -- fetch queries -- //
        const parseResultToScreens = result => this.service.getScreens(result.adapters, result.queryResults);
        const setVM = screens => {
            this.screens = screens;
            this.showCircles = this.screens.length > 1;
        };
        const refresh = () => this.service
            .fetchPreviousQueries()
            .then(parseResultToScreens)
            .then(setVM)
            .catch(error => console.log(error));

        // -- add i2b2 event listener -- //
        const isMinimized = e => e.action !== 'ADD';
        i2b2Svc.onResize((a, b) => this.vertStyle = b.find(isMinimized) ? 'v-min' : 'v-full');
        i2b2Svc.onHistory(refresh);

        // -- init -- //
        refresh();
    }
}



