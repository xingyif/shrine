(function () {

    angular.module('shrine.steward.statistics')
        .factory('OntologyTerm', OntologyTermFactory);

    function OntologyTermFactory() {
        return OntologyTerm;
    }

    function OntologyTerm(key) {
        this.key = key;
        this.children = {};
        this.queries = {};
        this.queryCount = 0;
    }

    /* static */
    OntologyTerm.prototype.maxTermUsedCount = 0;

    OntologyTerm.prototype.addQuery = function (queryId, queryData) {

        this.queryCount++;

        if (this.queryCount > OntologyTerm.prototype.maxTermUsedCount) {
                OntologyTerm.prototype.maxTermUsedCount = this.queryCount;
        }
        if (!this.queries[queryId]) {
            this.queries[queryId] = queryData;
        }
    };


    OntologyTerm.prototype.getQueries = function () {
        return Object.keys(this.queries);
    };

    OntologyTerm.prototype.getQueryCount = function () {
        return this.queryCount;
    };

    OntologyTerm.prototype.hasChildren = function () {
        return !!(Object.keys(this.children).length > 0);
    };

})();