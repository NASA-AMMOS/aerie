package gov.nasa.jpl.aerie.merlin.driver.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * An append-only list comprising a chain of fixed-size slabs.
 *
 * The fixed-size slabs allow for better cache locality when traversing the list forward,
 * and the chain of links allows for cheap extension when a slab reaches capacity.
 */
/*package-local*/ final class SlabList<T> implements Iterable<T> {
  /** ~4 KiB of elements (or at least, references thereof). */
  private static final int SLAB_SIZE = 1024;

  private final LinkedList<ArrayList<T>> slabs = new LinkedList<>();
  { this.slabs.addLast(new ArrayList<>(SLAB_SIZE)); }

  public void append(final T element) {
    final var lastSlab = this.slabs.getLast();
    lastSlab.add(element);

    if (lastSlab.size() >= SLAB_SIZE) {
      this.slabs.addLast(new ArrayList<>(SLAB_SIZE));
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SlabList<?> other)) return false;

    return Objects.equals(this.slabs, other.slabs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.slabs);
  }

  @Override
  public String toString() {
    return SlabList.class.getSimpleName() + "[" +
           "segments=" + this.slabs + ']';
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>() {
      private final Iterator<ArrayList<T>> slabIterator = SlabList.this.slabs.iterator();

      private Iterator<T> segmentIterator = Collections.emptyIterator();

      @Override
      public boolean hasNext() {
        ensureNext();
        return this.segmentIterator.hasNext();
      }

      @Override
      public T next() {
        ensureNext();
        return this.segmentIterator.next();
      }

      private void ensureNext() {
        // TERMINATION: The list of slabs is finite.
        while (true) {
          if (this.segmentIterator.hasNext()) break;
          if (!this.slabIterator.hasNext()) break;

          this.segmentIterator = this.slabIterator.next().iterator();
        }
      }
    };
  }
}
