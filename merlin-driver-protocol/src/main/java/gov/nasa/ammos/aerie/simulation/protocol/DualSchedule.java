package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DualSchedule {
  Schedule schedule;
  List<Edit> edits;

  public sealed interface Edit {
    Schedule apply(Schedule original);

    record Update(long id, Duration newStartOffset) implements Edit {
      @Override
      public Schedule apply(final Schedule original) {
        return original.setStartTime(id, newStartOffset);
      }
    }
    record Delete(long id) implements Edit {
      @Override
      public Schedule apply(final Schedule original) {
        return original.delete(id);
      }
    }
    record Add(Duration startOffset, String directiveType) implements Edit {
      @Override
      public Schedule apply(final Schedule original) {
        return original.plus(Schedule.build(Pair.of(startOffset, new Directive(directiveType, Map.of()))));
      }
    }
  }

  public DualSchedule() {
    schedule = Schedule.empty();
    edits = new ArrayList<>();
  }

  public Modifier add(Duration startOffset, String directiveType) {
    schedule = schedule.plus(Schedule.build(Pair.of(startOffset, new Directive(directiveType, Map.of()))));
    final var id = schedule.entries().getLast().id();
    return new Modifier() {
      @Override
      public void thenUpdate(final Duration newStartOffset) {
        edits.add(new Edit.Update(id, newStartOffset));
      }

      @Override
      public void thenDelete() {
        edits.add(new Edit.Delete(id));
      }
    };
  }

  public void thenAdd(Duration startOffset, String directiveType) {
    edits.add(new Edit.Add(startOffset, directiveType));
  }

  public void thenDelete(long id) {
    edits.add(new Edit.Delete(id));
  }

  public void thenUpdate(long id, Duration newStartOffset) {
    edits.add(new Edit.Update(id, newStartOffset));
  }

  public interface Modifier {
    void thenUpdate(Duration newStartOffset);
    void thenDelete();
  }

  public Schedule schedule1() {
    schedule.entries().sort(Comparator.comparing(Schedule.ScheduleEntry::startOffset));
    return schedule;
  }

  public Schedule schedule2() {
    var res = schedule;
    for (final var edit : edits) {
      switch (edit) {
        case Edit.Add e -> {
          res = res.plus(Schedule.build(Pair.of(e.startOffset, new Directive(e.directiveType, Map.of()))));
        }
        case Edit.Delete e -> {
          res = res.delete(e.id);
        }
        case Edit.Update e -> {
          res = res.setStartTime(e.id, e.newStartOffset);
        }
      }
    }
    res.entries().sort(Comparator.comparing(Schedule.ScheduleEntry::startOffset));
    return res;
  }

  public List<Pair<Schedule.ScheduleEntry, Edit>> summarize() {
    final var res = new ArrayList<Pair<Schedule.ScheduleEntry, Edit>>();
    final var entriesById = new LinkedHashMap<Long, Schedule.ScheduleEntry>();
    final var editsById = new LinkedHashMap<Long, Edit>();
    final var thenAdds = new ArrayList<Edit.Add>();
    for (final var entry : schedule.entries()) {
      entriesById.put(entry.id(), entry);
    }
    for (final var edit : edits) {
      switch (edit) {
        case Edit.Add e -> {
          thenAdds.add(e);
        }
        case Edit.Delete e -> {
          editsById.put(e.id(), e);
        }
        case Edit.Update e -> {
          editsById.put(e.id(), e);
        }
      }
    }

    for (final var entry : entriesById.entrySet()) {
      final var edit = editsById.get(entry.getKey());
      res.add(Pair.of(entry.getValue(), edit));
    }

    for (final var add : thenAdds) {
      res.add(Pair.of(null, add));
    }

    return res;
  }
}
