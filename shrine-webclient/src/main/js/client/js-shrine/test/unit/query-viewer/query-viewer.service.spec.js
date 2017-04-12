
import { QueryViewerService } from '../../../src/views/query-viewer/query-viewer.service';
const asyncQueryResult = require('./async-queries-mock');
console.log(asyncQueryResult);
describe('queryViewerService and asyncQueryResult should be initialized', () => {
    const queries = asyncQueryResult.queryResults;
    const nodes = asyncQueryResult.adapters;
    const service = new QueryViewerService();

    it('Should calculate the last node index for the screen.', () => {
        const maxNodesPerScreen = 10;
        const startIndex = 0;
        const expectedResult = 6;
        const result = service.getNumberOfNodesOnScreen(nodes, startIndex, maxNodesPerScreen);
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
        const node = 'SHRINE QA Hub';
        const expectedResult = [{ name: 'Admit Diagnosis@14:29:22', count: 'not available' },
        { name: 'Female@19:46:33', count: 50 },
        { name: '0-9 years old@19:46:24', count: -1 },
        { name: 'Female@19:46:17', count: 55 },
        { name: '0-9 years old@19:46:03', count: -1 }];
        const result = service.findQueriesForNode(node, queries);
        expectedResult.forEach((e, i) => {
            const r = result[i];
            expect(e.name + ':' + e.count).toBe(r.name + ':' + r.count);
        });
    });


    it('Should map queries to screen nodes', () => {
        const startIndex = 0;
        const endIndex = 5;
        const node = 'SHRINE QA Hub';
        const expectedMap = new Map();
        expectedMap.set(node, [{ name: 'Admit Diagnosis@14:29:22', count: 'not available' },
        { name: 'Female@19:46:33', count: 50 },
        { name: '0-9 years old@19:46:24', count: -1 },
        { name: 'Female@19:46:17', count: 55 },
        { name: '0-9 years old@19:46:03', count: -1 }]); 
        const expectedResult = expectedMap.get(node);
        const resultMap = service.mapQueriesToScreenNodes(nodes, queries);
        const result = resultMap.get(node);
        expectedResult.forEach((e, i) => {
            const r = result[i];
            expect(e.name + ':' + e.count).toBe(r.name + ':' + r.count);
        });
    });

    it('Should get all screens', () => {
        const screenIndex = 0;
        const maxNodes = 10;
        const expectedScreenId = 'S-s';
        const node = 'SHRINE QA Hub';
        const expectedMap = new Map();
        expectedMap.set(node, [{ name: 'Admit Diagnosis@14:29:22', count: 'not available' },
        { name: 'Female@19:46:33', count: 50 },
        { name: '0-9 years old@19:46:24', count: -1 },
        { name: 'Female@19:46:17', count: 55 },
        { name: '0-9 years old@19:46:03', count: -1 }]);
        const expectedResult = expectedMap.get(node);
        const resultScreens = service.parseScreens(nodes, queries);
        const resultScreen = resultScreens.get(expectedScreenId);
        const result = resultScreen.get(node);
        expectedResult.forEach((e, i) => {
            const r = result[i];
            expect(e.name + ':' + e.count).toBe(r.name + ':' + r.count);
        });
    });
});

