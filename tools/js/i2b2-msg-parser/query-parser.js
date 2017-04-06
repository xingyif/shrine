var dom = require('./i2b2-dom');
var HiveHelper = require('./hive-helper');
var url = './html/query-parser.html';
var window, document, $;

dom.load(url, [
    'https://cdnjs.cloudflare.com/ajax/libs/jquery/3.0.0/jquery.js'
], domLoaded);

function domLoaded(win) {

    // -- initial setup -- //
    window = win;
    document = window.document;
    $ = window.$;

    dom.loadData('query-contents.json')
        .then(function (result) {
            var data = JSON.parse(result.data);
            var queryRecords = data.queryRecords;
            var ln = queryRecords.length;
            var queryContentsList = [];
            var terms = new OntologyTerm('root');
            var obj;

            // all query records.
            for (var i = 0; i < ln; i++) {
                var record = queryRecords[i];
                var str = record.queryContents;
                terms = termBuilder(str.queryDefinition.expr, record.externalId, terms);
            }
        });
}


/*
create local terms object and append it to global object.
push id to queryList only if term does not exist yet.
something like terms = {
    "\\SHRINE\SHRINE\Demographics\Age\0-9 years old\3 years old\": queryList []
}
*/
function termBuilder(obj, queryId, terms) {

    var keys = Object.keys(obj);

    // -- nothing to search, we're done. -- //
    if (typeof (obj) !== 'object' || !keys.length) {
        return terms;
    }

    for (var i = 0; i < keys.length; i++) {
        var key = keys[i];

        if (key === 'term') {

            //if not an array and has been added already, push the queryId onto the list.
            //if is an array, iterate over the array.  push the queryId onto the list.
            //if not an array and has not been added already, create term object, push the queryId onto the list.
            //if not an array and has already been added, push the queryId onto the list, will pull count of uniques later. 


            //

            // -- if not an arry and has not been added.
            if (!Array.isArray(obj.term)) {
                processKey(terms, obj.term, queryId);
            }
            else {
                // -- iterate through the termlist -- //
                var termList = obj.term;
                for (var j = 0; j < termList.length; j++) {
                    var term = termList[j];
                    processKey(terms, term, queryId);
                }
            }
        }
        else {
            termBuilder(obj[key], queryId, terms);
        }
    }

    return terms;
}


function processKey(terms, key, queryId) {
    var prunedKey = key.split('\\\\SHRINE\\SHRINE')[1];
    var ontologyList = prunedKey.split('\\').slice(1);
    ontologyList = ontologyList.slice(0, ontologyList.length - 1);

    var currentLevel = terms;
    for (var i = 0; i < ontologyList.length; i++) {
        var term = ontologyList[i];
        processTerm(currentLevel, term, queryId);
        //currentLevel = currentLevel[term];
        currentLevel = currentLevel.children[term];
    }
}


function processTerm(terms, key, queryId) {
    var term;

    if (!terms.children[key]) {
        term = addTerm(terms, key, queryId);
    }
    else {
        term = terms.children[key];
        term.addQuery(queryId, queryId);
    }
}

//todo: make method more singlar and focused.
function addTerm(terms, key, queryId) {

    var term = new OntologyTerm(key);
    term.addQuery(queryId, queryId);
    terms.children[key] = term;
    return term;
}

function OntologyTerm(key) {
    this.key = key;
    this.children = {};
    this.queries = {};
}

OntologyTerm.prototype.addQuery = function(queryId, queryData) {
    this.queries[queryId] = queryData;
};

OntologyTerm.prototype.getQueries = function () {
    return Object.keys(this.queries);
};

OntologyTerm.prototype.getQueryCount = function () {
    return this.getQueries().length;
};


OntologyTerm.prototype.hasChildren = function () {
    return !!Object.keys(this.children).length > 0;
};