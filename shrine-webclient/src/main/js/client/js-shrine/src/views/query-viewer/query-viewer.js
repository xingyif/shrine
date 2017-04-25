import { inject, computedFrom } from 'aurelia-framework';
import { QueryViewerService } from 'views/query-viewer/query-viewer.service';

@inject(QueryViewerService)
export class QueryViewer {
    constructor(service) {
        this.screenIndex = 0;
        this.showCircles = false;
        this.service = service;
        this.service
            .fetchPreviousQueries()
            .then(result => {
                const queries = result.queryResults;
                const nodes = result.adapters  
                return service.getScreens(nodes, queries);
            })
            .then(screens => {
                this.screens = screens;
                this.showCircles = this.screens.length > 1;
            })
            .catch(error => console.log(error));
    }
}


