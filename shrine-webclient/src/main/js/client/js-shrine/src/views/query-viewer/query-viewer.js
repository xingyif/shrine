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
        this.runningQuery = null;
        this.service = service;
        this.vertStyle = 'v-min';
        this.scrollRatio = 0;

        // -- fetch queries -- //
        const parseResultToScreens = result => this.service.getScreens(result.adapters, result.queryResults);
        const setVM = screens => {
            this.showLoader = false;
            this.runningQuery = null;
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

        const addQuery = (event, data) => this.runningQuery = data[0].name;
        const init = () => (model.isLoaded) ? setVM(model.screens) : refresh();

        // -- add i2b2 event listener -- //
        const isMinimized = e => e.action !== 'ADD';
        const setVertStyle = (a, b) => this.vertStyle = b.find(isMinimized) ? 'v-min' : 'v-full';
        i2b2Svc.onResize(setVertStyle);
        i2b2Svc.onHistory(refresh);
        i2b2Svc.onQuery(addQuery);
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

    onScroll(e) {
        this.scrollRatio = (e.target.clientHeight + e.target.scrollTop)  + '/' + e.target.scrollHeight;
    }
}



