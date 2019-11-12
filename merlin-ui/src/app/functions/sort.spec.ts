import { compare } from './sort';

describe('sort', () => {
  it('compare: a < b and isAsc', () => {
    const res = compare(0, 1, true);
    expect(res).toEqual(-1);
  });

  it('compare: a < b and !isAsc', () => {
    const res = compare(0, 1, false);
    expect(res).toEqual(1);
  });

  it('compare: a > b and isAsc', () => {
    const res = compare(1, 0, true);
    expect(res).toEqual(1);
  });

  it('compare: a > b and !isAsc', () => {
    const res = compare(1, 0, false);
    expect(res).toEqual(-1);
  });
});
