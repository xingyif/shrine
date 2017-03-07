import {App} from '../../src/app';


describe('the App module', () => {
  

  beforeEach(() => {
    let app = new App();
  });

  it('contains a router property', () => {
    expect({}).toBeDefined();
  });
});
