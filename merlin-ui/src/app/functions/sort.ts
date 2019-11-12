export function compare(
  a: number | string,
  b: number | string,
  isAsc: boolean,
): number {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}
