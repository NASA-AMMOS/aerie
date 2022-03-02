export function findDuplicates<T, S>(array: T[], mapper?: (item: T) => S ): T[] {
  const seen = new Set<T | S>();
  const duplicated = new Set<T|S>();
  const result = [];
  for (const item of array) {
    const mappedItem = mapper ? mapper(item) : item;
    if (duplicated.has(mappedItem)) {
      continue;
    }
    if (seen.has(mappedItem)) {
      duplicated.add(mappedItem);
      result.push(...array.filter(i => mapper ? mapper(i) === mappedItem : i === mappedItem));
    } else {
      seen.add(mappedItem);
    }
  }
  return result;
}
