import * as spec from 'tape';

spec.test("Is Tape Testing?: ", (t) => {
  const app = {};
  t.true(app !== undefined);
  t.end();
});