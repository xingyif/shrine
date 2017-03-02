import {App, App2} from 'src/app.js';

describe('A test of app.js ', () => {
    it('should not work',  () => {
        var app = new App();
        expect(app.message).toBe('Hello Aurelia');
    });
});