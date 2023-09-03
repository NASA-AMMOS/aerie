import {coalesce, Segment} from "./segment";
import {Inclusivity, Interval} from "./interval";
import {Windows} from "./windows";
import {Timeline,} from "./timeline";
import {BinaryOperation} from "./binary-operation";
import {LinearEquation, Real} from "./real";

export enum ProfileType {
  Real,
  Discrete,
  Windows,
  Other
}

export class Profile<V> {
  private segments: Timeline<Segment<V>>;
  private readonly typeTag: ProfileType;

  constructor(segments: Timeline<Segment<V>>, typeTag: ProfileType) {
    this.segments = segments;
    this.typeTag = typeTag;
  }

  public map(f: (v: Segment<V>) => Segment<V>, boundsMap: (b: Interval) => Interval): ProfileSpecialization<V>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, boundsMap: (b: Interval) => Interval, type_tag: ProfileType): ProfileSpecialization<W>;
  public map<W>(f: (v: Segment<V>) => Segment<W>, boundsMap: (b: Interval) => Interval, type_tag?: ProfileType): ProfileSpecialization<W> {
    if (type_tag === undefined) {
      type_tag = this.typeTag;
    }
    let result = (new Profile<W>(bounds => this.segments(boundsMap(bounds)).map(f), type_tag)).specialize();
    result.coalesce();

    return result;
  }

  public mapValues(f: (v: Segment<V>) => V): ProfileSpecialization<V>;
  public mapValues<W>(f: (v: Segment<V>) => W, type_tag: ProfileType): ProfileSpecialization<W>;
  public mapValues<W>(f: (v: Segment<V>) => W, type_tag?: ProfileType): ProfileSpecialization<W> {
    return this.map<W>(
        s => new Segment(s.interval, f(s)),
        $ => $,
        type_tag !== undefined ? type_tag : this.typeTag
    );
  }

  public mapIntervals(map: (s: Segment<V>) => Interval, boundsMap: (b: Interval) => Interval): ProfileSpecialization<V> {
    return this.map<V>(
        s => new Segment<V>(f(s), s.value),
        boundsMap,
        this.typeTag
    );
  }

  public map2Values<W, Result>(
      rightProfile: Profile<W>,
      op: BinaryOperation<V, W, Result>,
      typeTag: ProfileType
  ): ProfileSpecialization<Result> {
    let leftProfile = this;
    let segments = (bounds: Interval) => {
      let left = leftProfile.segments(bounds);
      let right = rightProfile.segments(bounds);

      let result: Segment<Result>[] = [];

      let leftIndex = 0;
      let rightIndex = 0;

      let leftSegment: Segment<Left> | undefined = undefined;
      let rightSegment: Segment<Right> | undefined = undefined;
      let remainingLeftSegment: Segment<Left> | undefined = undefined;
      let remainingRightSegment: Segment<Right> | undefined = undefined;

      while (leftIndex < left.length && rightIndex < right.length && remainingLeftSegment !== undefined && remainingRightSegment !== undefined) {
        if (remainingLeftSegment !== undefined) {
          leftSegment = remainingLeftSegment;
          remainingLeftSegment = undefined;
        } else if (leftIndex < left.length) {
          leftSegment = left[leftIndex++];
        }
        if (remainingRightSegment !== undefined) {
          rightSegment = remainingRightSegment;
          remainingRightSegment = undefined;
        } else if (rightIndex < right.length) {
          rightSegment = right[rightIndex++];
        }

        if (leftSegment === undefined) {
          let resultingSegment = rightSegment!.mapValue(op.right).transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else if (rightSegment === undefined) {
          let resultingSegment = leftSegment!.mapValue(op.left).transpose();
          if (resultingSegment !== undefined) result.push(resultingSegment);
        } else {
          let startComparison = Interval.compareStarts(leftSegment.interval, rightSegment.interval);
          if (startComparison === -1) {
            remainingRightSegment = rightSegment;
            let endComparison = Interval.compareEndToStart(leftSegment.interval, rightSegment.interval);
            if (endComparison < 1) {
              let resultingSegment = leftSegment.mapValue(op.left).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            } else {
              remainingLeftSegment = leftSegment.mapInterval(i => Interval.intersect(i, rightSegment!.interval));
              let resultingSegment = new Segment(
                  Interval.between(leftSegment.interval.start, rightSegment!.interval.start, leftSegment.interval.startInclusivity, Inclusivity.opposite(rightSegment!.interval.startInclusivity)),
                  op.left(leftSegment.value)
              ).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            }
          } else if (startComparison === 1) {
            remainingLeftSegment = leftSegment;
            let endComparison = Interval.compareEndToStart(rightSegment.interval, leftSegment.interval);
            if (endComparison < 1) {
              let resultingSegment = rightSegment.mapValue(op.right).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            } else {
              remainingRightSegment = rightSegment.mapInterval(i => Interval.intersect(i, leftSegment!.interval));
              let resultingSegment = new Segment(
                  Interval.between(rightSegment.interval.start, leftSegment!.interval.start, rightSegment.interval.startInclusivity, Inclusivity.opposite(leftSegment!.interval.startInclusivity)),
                  op.right(rightSegment.value)
              ).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            }
          } else {
            let endComparison = Interval.compareEnds(leftSegment.interval, rightSegment.interval);
            if (endComparison === -1) {
              remainingRightSegment = rightSegment.mapInterval(i => Interval.between(leftSegment!.interval.end, i.end, Inclusivity.opposite(leftSegment!.interval.endInclusivity), i.endInclusivity));
              let resultingSegment = leftSegment.mapValue(l => op.combine(l, rightSegment!.value)).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            } else if (endComparison === 1) {
              remainingLeftSegment = leftSegment.mapInterval(i => Interval.between(rightSegment!.interval.end, i.end, Inclusivity.opposite(rightSegment!.interval.endInclusivity), i.endInclusivity));
              let resultingSegment = rightSegment.mapValue(r => op.combine(leftSegment!.value, r)).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            } else {
              let resultingSegment = leftSegment.mapValue(l => op.combine(l, rightSegment!.value)).transpose();
              if (resultingSegment !== undefined) result.push(resultingSegment);
            }
          }
        }
      }
      return result;
    }

    return (new Profile(segments, typeTag)).specialize();
  }

  public compareValues<W>(other: Profile<W>, comparator: (l: V, r: W) => boolean): Windows {
    return this.map2Values(other, BinaryOperation.combineOrUndefined(comparator), ProfileType.Windows);
  }

  public filter(f: (s: Segment<V>) => boolean): ProfileSpecialization<V> {
    let segments = (bounds: Interval) => this.segments(bounds).filter(f);
    return (new Profile<V>(segments, this.typeTag)).specialize();
  }

  public edges(edgeFilter: BinaryOperation<V, V, boolean>): Windows {
    let newSegments = (bounds: Interval) => {
      let result: Segment<boolean>[] = [];
      let buffer: Segment<V> | undefined = undefined;
      return this.segments(bounds).flatMap(
          currentSegment => {
            let leftEdge: boolean | undefined;
            let rightEdge: boolean | undefined;

            let previous = buffer;
            buffer = currentSegment;
            let currentInterval = currentSegment.interval;

            if (currentInterval.end === bounds.end && currentInterval.endInclusivity === bounds.endInclusivity) {
              if (bounds.includesEnd()) rightEdge = false;
              else rightEdge = undefined;
            } else {
              rightEdge = edgeFilter.left(currentSegment.value);
            }

            if (previous !== undefined) {
              if (Interval.compareEndToStart(previous.interval, currentInterval) === 0) {
                leftEdge = edgeFilter.combine(previous.value, currentSegment.value);
              } else {
                leftEdge = edgeFilter.right(currentSegment.value);
              }
            } else {
              if (currentInterval.start == bounds.start && currentInterval.startInclusivity == bounds.startInclusivity) {
                if (bounds.includesStart()) leftEdge = false;
                else leftEdge = undefined;
              } else {
                leftEdge = edgeFilter.right(currentSegment.value);
              }
            }

            return [
              (new Segment(
                  Interval.at(currentInterval.start),
                  leftEdge
              )).transpose(),
              (new Segment(
                  Interval.between(currentInterval.start, currentInterval.end, Inclusivity.Exclusive),
                  false
              )).transpose(),
              (new Segment(
                  Interval.at(currentInterval.end),
                  rightEdge
              )).transpose()
            ];
          }
      ).filter($ => $ !== undefined) as Segment<boolean>[];
    };
    return new Windows(newSegments);
  }

  public allEdges(): Windows {
    return this.edges(BinaryOperation.combineOrUndefined((l, r) => l !== r));
  }

  public specificEdges(from: V, to: V): Windows {
    return this.edges(BinaryOperation.cases(
        l => l === from ? undefined : false,
        r => r === to ? undefined : false,
        (l, r) => l === from && r === to
    ));
  }

  public async collect(bounds: Interval): Promise<Segment<V>[]> {
    return this.segments(bounds);
  }

  public select(selection: Interval): ProfileSpecialization<V> {
    let segments = (bounds: Interval) => this.segments(Interval.intersect(selection, bounds));
    return (new Profile(segments, this.typeTag)).specialize();
  }

  public specialize(): ProfileSpecialization<V> {
    if (this.typeTag === ProfileType.Windows) {
      // @ts-ignore
      return new Windows(this);
    } else if (this.typeTag === ProfileType.Real) {
      // @ts-ignore
      return new Real(this);
    }

    // @ts-ignore
    return this;
  }

  public coalesce() {
    this.segments = bounds => coalesce<V>(this.segments(bounds), (l, r) => l === r);
  }
}

type ProfileSpecialization<V> =
  V extends boolean ? Windows
  : V extends LinearEquation ? Real
  : Profile<V>;
