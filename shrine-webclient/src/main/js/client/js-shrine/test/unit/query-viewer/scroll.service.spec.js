import {ScrollService} from '../../../src/views/query-viewer/scroll.service';

describe('ScrollService should exist', () => {

    const service = ScrollService;

    it('Should be undefined', () => {
        
        const event = {target:{
            scrollTop: 100,
            scrollHeight: 200,
            clientHeight: 100
        }};
        const result = ScrollService.scrollRatio(event);
       
        expect(result.value).toBe(1);

    });

});