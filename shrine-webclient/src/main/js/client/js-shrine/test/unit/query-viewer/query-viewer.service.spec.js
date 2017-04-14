
import { QueryViewerService } from '../../../src/views/query-viewer/query-viewer.service';
import { QueryViewerConfig } from '../../../src/views/query-viewer/query-viewer.config';
const asyncQueryResult = require('./async-queries-mock');

describe('queryViewerService and asyncQueryResult should be initialized', () => {
    const queries = asyncQueryResult.queryResults;
    const nodes = asyncQueryResult.adapters;
    const service = new QueryViewerService({}, QueryViewerConfig);

    it('Should calculate the last node index for the screen.', () => {
        const maxNodesPerScreen = 10;
        const startIndex = 0;
        const expectedResult = 6;
        const result = service.getNumberOfNodesOnScreen(nodes, startIndex);
        expect(result).toBe(expectedResult);
    });

    it('Should generate the screen id.', () => {
        const startIndex = 0;
        const endIndex = 5;
        const expectedResult = 'S-s';
        const result = service.getScreenId(nodes, startIndex, endIndex);
        expect(result).toBe(expectedResult);
    });

    it('Should find queries for a node', () => {
        const node = 'shrine-qa1';
        const result = service.mapQueriesToScreenNodes([node], queries);
        expect(result.length).toBeGreaterThan(0);
    });
});
