import { Profile, Windows, Real, LinearEquation, Timeline, Segment } from '../internal.js';

export enum ProfileType {
  Real,
  Windows,
  Other
}

export type ProfileSpecialization<V> = V extends boolean ? Windows : V extends LinearEquation ? Real : Profile<V>;

export namespace ProfileType {
  export function specialize<V>(p: Profile<V>, t: ProfileType): ProfileSpecialization<V> {
    if (t === ProfileType.Windows) {
      // @ts-ignore
      return new Windows(p.segments as Timeline<Segment<boolean>>);
    } else if (t === ProfileType.Real) {
      // @ts-ignore
      return new Real(p.segments as Timeline<Segment<LinearEquation>>);
    }

    // @ts-ignore
    return p;
  }
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
