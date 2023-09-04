import type {LinearEquation} from "./real";

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
}
