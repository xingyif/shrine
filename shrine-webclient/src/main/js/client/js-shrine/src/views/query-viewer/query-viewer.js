import { inject } from 'aurelia-framework';
import { QueryViewerService } from 'views/query-viewer/query-viewer.service';

// -- config -- //
const nodesPerScreen = 5;

@inject(QueryViewerService)
export class QueryViewer {
    constructor(service) {
        this.screenIndex = 0;
        this.service = service;
        this.nodes = [];
        this.service
            .fetchPreviousQueries()
            .then(result => {
                // -- save results and parse data -- //
                this.queries = result.queries;
                if (this.nodes.length === 0) {
                    this.nodes = getNodes(this.queries);
                }

                // -- to be handled on click, pass in index -- //
                this.screenIndex = 1;
                this.screenNodes = this.nodes.slice(this.sliceStart, this.sliceEnd);
                this.testQueries = sliceResultsForScreen(this.queries, this.sliceStart, this.sliceEnd)

            })
            .catch(error => console.log(error));
    }

//--  @todo: this is a test -- //
    get screens() {
        const lastNodeIndex = this.nodes.length;
        let testResult = [];
        
        for(let i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
            let start = this.nodes[i];
            let endIndex = (i + nodesPerScreen < lastNodeIndex)? i + nodesPerScreen : lastNodeIndex -1;
            let end = this.nodes[endIndex];
            testResult.push(String(start).substr(0, 1) + '-' +  String(end).substr(0,1));
        }
        
        return testResult;
    }

    get sliceStart() {
        return this.screenIndex * nodesPerScreen;
    }

    get sliceEnd() {
        return this.sliceStart + nodesPerScreen;
    }
}

function getNodes(queries) {
    return (queries.length > 0) ?
        queries[0].results.map(result => result.node) : [];
}

function sliceResultsForScreen(queries, start, end) {
    return queries.map(query => {
        let q = Object.assign({}, query);
        q.results = query.results.slice(start, end);
        return q;
    });
}

