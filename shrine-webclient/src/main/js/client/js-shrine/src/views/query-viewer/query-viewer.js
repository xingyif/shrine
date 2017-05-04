import { inject, computedFrom } from 'aurelia-framework';
import { QueryViewerService } from 'views/query-viewer/query-viewer.service';
import { I2B2Service } from 'common/i2b2.service.js';
import { QueryViewerModel } from './query-viewer.model';

@inject(QueryViewerService, I2B2Service, QueryViewerModel)
export class QueryViewer {
    constructor(service, i2b2Svc, model) {

        // -- init -- //
        this.screenIndex = 0;
        this.showCircles = false;
        this.showLoader = true;
        this.service = service;
        this.vertStyle = 'v-min';

        // -- fetch queries -- //
        const parseResultToScreens = result => this.service.getScreens(result.adapters, result.queryResults);
        const setVM = screens => {
            this.showLoader = false;
            this.screens = screens;
            this.showCircles = this.screens.length > 1;
            model.screens = screens;
            model.isLoaded = true;
        };
        const refresh = () => this.service
            .fetchPreviousQueries()
            .then(parseResultToScreens)
            .then(setVM)
            .catch(error => console.log(error));

        const init = () => (model.isLoaded) ? setVM(model.screens) : refresh();

        // -- add i2b2 event listener -- //
        const isMinimized = e => e.action !== 'ADD';
        const setVertStyle = (a, b) => this.vertStyle = b.find(isMinimized) ? 'v-min' : 'v-full';
        i2b2Svc.onResize(setVertStyle);
        i2b2Svc.onHistory(refresh);
        init();
    }
    
    getContext(event, result) {
        return {
            x: event.pageX,
            y: event.pageY,
            id: result.id,
            class: 'show'
        };
    }
}



