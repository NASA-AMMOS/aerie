import { LinearEquation } from './real';

export enum ProfileType {
  Real,
  Windows,
  Other
}

export namespace ProfileType {
  export function getSegmentComparator(t: ProfileType): (l: any, r: any) => boolean {
    if (t === ProfileType.Real) return (l: LinearEquation, r: LinearEquation) => l.equals(r);
    return (l: any, r: any) => l === r;
  }

  export function guessType<V>(v: V): ProfileType {
    if (v instanceof LinearEquation) return ProfileType.Real;
    if (typeof v === 'boolean') return ProfileType.Windows;
    else return ProfileType.Other;
  }
}
