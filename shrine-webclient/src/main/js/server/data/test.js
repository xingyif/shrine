const nodesPerScreen = 5;
const result = require('./async-queries.js');





function getNodes(queries) {
    return (queries.length > 0) ?
        queries[0].results.map(result => result.node) : [];
}


function getScreens2() {
    const lastNodeIndex = this.nodes.length;
    let testResult = [];

    for (let i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
        let start = this.nodes[i];
        let endIndex = (i + nodesPerScreen < lastNodeIndex) ? i + nodesPerScreen : lastNodeIndex - 1;
        let end = this.nodes[endIndex];
        testResult.push(String(start).substr(0, 1) + '-' + String(end).substr(0, 1));
    }

    return testResult;
}

function sliceResultsForScreen(queries, start, end) {
    return queries.map(query => {
        let q = Object.assign({}, query);
        q.results = query.results.slice(start, end);
        return q;
    });
}

function getScreens(nodes, queries) {
    const lastNodeIndex = nodes.length;
    let screens = [];

    for (let i = 0; i < lastNodeIndex; i = i + nodesPerScreen) {
        let endIndex = (i + nodesPerScreen < lastNodeIndex) ? i + nodesPerScreen : lastNodeIndex - 1;
        let screenId = String(nodes[i]).substr(0, 1) + '-' + String(nodes[endIndex]).substr(0, 1);
        let screenNodes = nodes.slice(i, endIndex);
        let screenQueries = queries.map(query => query.results.slice(i, endIndex));
        
        screens.push({
            name: screenId,
            nodes: screenNodes,
            queries: screenQueries
        });
    }

    return screens;
}


const nodes = getNodes(result.queries);
let testResult = getScreens(nodes, result.queries);


console.log(testResult);
console.log(testResult[0].queries[0]);