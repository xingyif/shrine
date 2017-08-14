import {Container} from '../../../src/services/container'

describe('services/container tests:', () => {

        let container;
        let value = {data: 'test'};
        beforeEach(() => {
            container = Container.of(value);
        });

        it('container should contain the value', () => {
            expect(container.value).toBe(value);   
        }); 

        it('container.map should apply a method to the a container that holds the modified value', () => {
           const result = container.map(v => `${v.data} mapped`);
           expect(result.value).toBe('test mapped');
        });

        it('container.join should provide access to the raw value directly', () => {
            value.data = 'test';
            const result = container.map(v => `${v.data} mapped`);
            expect(result.join()).toBe('test mapped');
        });

        it('container.chain should perform a map and then return the raw value', () => {
            value.data = 'test';
            const result = container.chain(v => `${v.data} chained`);
            expect(result).toBe('test chained');
        });
    });