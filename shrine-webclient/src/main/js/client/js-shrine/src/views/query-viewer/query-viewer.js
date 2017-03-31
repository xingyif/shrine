import { inject } from 'aurelia-framework';
import { QueryViewerService } from 'views/query-viewer/query-viewer.service';

// -- config -- //
const nodesPerScreen = 10;

@inject(QueryViewerService)
export class QueryViewer {
    constructor(service) {
        this.screenIndex = 0;
        this.service = service;
        this.service
            .fetchPreviousQueries()
            .then(result => {
                const queries = result.queries;
                const nodes = this.service.getNodes(queries);
                return service.getScreens(nodes, queries);
            })
            .then(screens => {
                this.screens = screens;
            })
            .catch(error => console.log(error));
    }

    attached() {
         this.boxWrapper = document.getElementsByClassName('box-wrapper')[0];
    }
    get slidePct() {
        return String(-100 * this.screenIndex) + '%';
    }
}


