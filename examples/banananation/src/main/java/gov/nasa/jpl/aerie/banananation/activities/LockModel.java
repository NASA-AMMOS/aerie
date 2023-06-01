package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import org.apache.commons.lang3.tuple.Pair;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.waitUntil;

public class LockModel<TaskId> {
  private final Queue<TaskId> queue;

  public LockModel(final Comparator<TaskId> taskIdComparator) {
    this.queue = new CellBackedQueue<>(taskIdComparator);
  }

  void lock(final TaskId taskId) {
    queue.add(taskId);
    waitUntil((positive, atEarliest, atLatest) -> {
      if (positive == (this.queue.peek() == taskId)) {
        return Optional.of(atEarliest);
      } else {
        return Optional.empty();
      }
    });
  }

  void release(final TaskId taskId) {
    if (queue.peek() != taskId) throw new IllegalStateException("Cannot release " + taskId + " because it does not hold the lock");
    queue.remove();
  }

  static class CellBackedQueue<T> extends AbstractQueue<T> {
    final CellRef<QueueAction<T>, QueueCell<T>> queueCell;

    CellBackedQueue(final Comparator<T> serializer) {
      this.queueCell = QueueCell.allocate(serializer);
    }

    @Override
    public Iterator<T> iterator() {
      return queueCell.get().iterator();
    }

    @Override
    public int size() {
      return queueCell.get().size();
    }

    @Override
    public boolean offer(final T t) {
      queueCell.emit(new QueueAction.Add<>(t));
      return true;
    }

    @Override
    public T poll() {
      if (queueCell.get().isEmpty()) return null;
      final var res = queueCell.get().first();
      queueCell.emit(new QueueAction.Pop<>());
      return res;
    }

    @Override
    public T peek() {
      if (queueCell.get().isEmpty()) return null;
      return queueCell.get().first();
    }
  }

  static class QueueCell<T> implements Iterable<T> {
    final List<T> list;

    QueueCell() {
      this.list = new LinkedList<>();
    }

    QueueCell(final QueueCell<T> other) {
     this.list = new LinkedList<>(other.list);
    }

    static <T> CellRef<QueueAction<T>, QueueCell<T>> allocate(final Comparator<T> serializer) {
      return CellRef.allocate(new QueueCell<>(), new CellType<>() {

        @Override
        public EffectTrait<QueueEffect<T>> getEffectType() {
          return new EffectTrait<>() {
            @Override
            public QueueEffect<T> empty() {
              return new QueueEffect.Empty<>();
            }

            @Override
            public QueueEffect<T> sequentially(final QueueEffect<T> prefix, final QueueEffect<T> suffix) {
              return new QueueEffect.Sequential<>(prefix, suffix);
            }

            @Override
            public QueueEffect<T> concurrently(final QueueEffect<T> left, final QueueEffect<T> right) {
              // All concurrent actions are smushed together and sorted using the comparator
              final var combined = new ArrayList<QueueAction<T>>();
              for (final var action : left) {
                combined.add(action);
              }
              for (final var action : right) {
                combined.add(action);
              }
              combined.sort((o1, o2) -> {
                if (o1 instanceof QueueAction.Pop<T>) return -1;
                if (o2 instanceof QueueAction.Pop<T>) return 1;
                if (o1 instanceof QueueAction.Empty<?>) return 1;
                if (o2 instanceof QueueAction.Empty<?>) return -1;

                final var t1 = ((QueueAction.Add<T>) o1).t();
                final var t2 = ((QueueAction.Add<T>) o2).t();

                return serializer.compare(t1, t2);
              });
              return QueueEffect.Sequential.of(combined);
            }
          };
        }

        @Override
        public QueueCell<T> duplicate(final QueueCell<T> tQueueCell) {
          return new QueueCell<>(tQueueCell);
        }

        @Override
        public void apply(final QueueCell<T> tQueueCell, final QueueEffect<T> tQueueEffect) {
          for (final var action : tQueueEffect) {
            if (action instanceof QueueAction.Empty<?>) continue;
            if (action instanceof QueueAction.Add<T> q) tQueueCell.list.add(q.t());
            if (action instanceof QueueAction.Pop<T> q) tQueueCell.list.remove(0);
          }
        }
      }, $ -> (QueueEffect<T>) new QueueEffect.Atom<>($));
    }

    @Override
    public Iterator<T> iterator() {
      return list.iterator();
    }

    public int size() {
      return this.list.size();
    }

    boolean isEmpty() {
      return this.list.isEmpty();
    }

    public T first() {
      if (this.list.isEmpty()) throw new NoSuchElementException();
      return this.list.get(0);
    }
  }

  sealed interface QueueAction<T> {
    record Empty<T>() implements QueueAction<T> {}
    record Add<T>(T t) implements QueueAction<T> {}
    record Pop<T>() implements QueueAction<T> {}
  }

  sealed interface QueueEffect<T> extends Iterable<QueueAction<T>> {
    record Empty<T>() implements QueueEffect<T> {
      @Override
      public Iterator<QueueAction<T>> iterator() {
        return Collections.emptyIterator();
      }
    }
    record Atom<T>(QueueAction<T> action) implements QueueEffect<T> {
      @Override
      public Iterator<QueueAction<T>> iterator() {
        return List.of(action).iterator();
      }
    }
    record Sequential<T>(QueueEffect<T> prefix, QueueEffect<T> suffix) implements QueueEffect<T> {
      Pair<QueueAction<T>, QueueEffect<T>> firstAndRest() {
        if (prefix instanceof QueueEffect.Empty<T>) {
          if (suffix instanceof QueueEffect.Empty<T>) {
            throw new IllegalStateException("There is no first and rest");
          }
          if (suffix instanceof QueueEffect.Atom<T> q) {
            return Pair.of(q.action, new Empty<>());
          }
          if (suffix instanceof QueueEffect.Sequential<T> q) {
            return q.firstAndRest();
          }
        }
        if (prefix instanceof QueueEffect.Atom<T> q) {
          return Pair.of(q.action, suffix);
        }
        if (prefix instanceof QueueEffect.Sequential<T> q) {
          final var prefixFirstAndRest = q.firstAndRest();
          return Pair.of(prefixFirstAndRest.getLeft(), new Sequential<>(prefixFirstAndRest.getRight(), suffix));
        }
        throw new Error("Whoops");
      }

      @Override
      public Iterator<QueueAction<T>> iterator() {
        return new Iterator<>() {
          QueueEffect<T> remaining = Sequential.this;
          @Override
          public boolean hasNext() {
            return !(remaining instanceof QueueEffect.Empty<T>);
          }

          @Override
          public QueueAction<T> next() {
            if (remaining instanceof QueueEffect.Empty<T>) throw new NoSuchElementException();
            if (remaining instanceof QueueEffect.Atom<T> q) {
              remaining = new Empty<>();
              return q.action();
            }
            if (remaining instanceof QueueEffect.Sequential<T> q) {
              final var firstAndRest = q.firstAndRest();
              remaining = firstAndRest.getRight();
              return firstAndRest.getLeft();
            }
            throw new Error("Oh boy");
          }
        };
      }

      static <T> QueueEffect<T> of(final Iterable<QueueAction<T>> other) {
        QueueEffect<T> res = new Empty<>();
        for (final var action : other) {
          res = new Sequential<>(res, new Atom<>(action));
        }
        return res;
      }
    }
  }
}
