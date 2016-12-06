(function () {
    'use strict';

    angular.module('shrine.steward.statistics')
        .service('OntologyTermService', OntologyTermService);

    OntologyTermService.$inject = ['OntologyTerm'];
    function OntologyTermService(OntologyTerm) {

        return {
            buildOntology: buildOntology,
            getMax: function () {
                var maxTermUsedCount = OntologyTerm.prototype.maxTermUsedCount;
                OntologyTerm.prototype.maxTermUsedCount = 0;
                return maxTermUsedCount;
            }
        };

        /**
         * Build Ontology from array of Query Records.
         */
        function buildOntology(queryRecords, topicId) {
            var ln = queryRecords.length;
            var ontology = new OntologyTerm('SHRINE');
            var topics = {};
            var parsingAllTopics = topicId === undefined;
            var filteringByTopic;
            var filteredCount = 0;

            for (var i = 0; i < ln; i++) {
                var record = queryRecords[i];
                var topic = record.topic;
                filteringByTopic = !parsingAllTopics && (topic && topicId === topic.id);

                appendTopicIfUnique(topic, topics);

                if (parsingAllTopics || filteringByTopic) {
                    var str = record.queryContents;
                    ontology = traverse(str.queryDefinition.expr || str.queryDefinition.subQuery, record.externalId, ontology);
                    filteredCount++;
                }
            }

            ontology.userName = (ln) ? queryRecords[0].user.userName : '';
            if (parsingAllTopics) {
                ontology.topics = formatTopicsToArray(topics);
            }
            ontology.queryCount = filteringByTopic ? filteredCount : ln;
            return ontology;
        }

        function formatTopicsToArray(topicObject) {
            var topicArray = [];
            var keys = Object.keys(topicObject);
            Object.keys(topicObject)
                .forEach(function (key) { topicArray.push(topicObject[key]); });
            return topicArray;
        }

        function appendTopicIfUnique(topic, topics) {

            if (!topic) {
                return;
            }

            var id = topic.id;

            if (!topics[id]) {
                topics[id] = topic;
            }

            return topics;
        }

        /**
         * Recursive Traversal of ontological hierarchy.
         */
        function traverse(obj, queryId, terms) {

            var keys = Object.keys(obj);

            // -- nothing to search, we're done. -- //
            if (typeof (obj) !== 'object' || !keys.length) {
                return terms;
            }

            // -- traverse each object key, parse all the terms. -- //
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];

                if (key === 'term') {

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
                    traverse(obj[key], queryId, terms);
                }
            }

            return terms;
        }

        /**
         * Parse out a term hierarchy of a term key.
         */
        function processKey(terms, key, queryId) {
            var prunedKey = key.split('\\\\SHRINE\\SHRINE')[1];
            var ontologyList = prunedKey.split('\\').slice(1);
            ontologyList = ontologyList.slice(0, ontologyList.length - 1);

            var currentLevel = terms;
            for (var i = 0; i < ontologyList.length; i++) {
                var term = ontologyList[i];
                processTerm(currentLevel, term, queryId);
                currentLevel = currentLevel.children[term];
            }
        }

        /**
         * If a term is already a child of the parent, then add it's query to the list of queries for that term.
         * Otherwise, create a new term object and add it to the parent.
         */
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

        /**
         * Add an instance of a query term to it's parent in the ontolgy.
         */
        function addTerm(terms, key, queryId) {

            var term = getNewTerm(key);
            term.addQuery(queryId, queryId);
            terms.children[key] = term;
            return term;
        }

        /**
         * Return Instance of Ontology Term.
         */
        function getNewTerm(name) {
            return new OntologyTerm(name);
        }

    }
})();