import { CommandStem, Sequence } from './lib/codegen/CommandEDSLPreface.js';
import type { SeqBuilder } from './types/seqBuilder';

export const defaultSeqBuilder: SeqBuilder = (
  sortedActivityInstancesWithCommands,
  seqId,
  seqMetadata,
  simulationDatasetId,
) => {
  // A combined list of all the activity instance commands.
  let allCommands: CommandStem<{} | []>[] = [];
  let activityInstaceCount = 0;
  let planId;
  let timeSorted = false;

  for (const ai of sortedActivityInstancesWithCommands) {
    // If errors, no associated Expansion
    if (ai.errors !== null) {
      planId = ai?.simulationDataset.simulation?.planId;

      if (ai.errors.length > 0) {
        allCommands = allCommands.concat(
          ai.errors.map(e =>
            CommandStem.new({
              stem: '$$ERROR$$',
              arguments: [{ message: e.message }],
            }).METADATA({ simulatedActivityId: ai.id }),
          ),
        );
      }

      // Typeguard only
      if (ai.commands === null) {
        break;
      }

      allCommands = allCommands.concat(ai.commands);
      // Keep track of the number of times we add commands to the allCommands list.
      activityInstaceCount++;
    }
  }

  // Now that we have all the commands for each activity instance, sort if there's > 1 activity instance.
  if (activityInstaceCount > 1) {
    const result = sortCommandsByTime(allCommands);

    timeSorted = result.timeSorted;
    allCommands = result.sortedCommands;
  }

  return Sequence.new({
    seqId: seqId,
    metadata: {
      ...seqMetadata,
      planId,
      simulationDatasetId,
      timeSorted,
    },
    steps: allCommands,
  });
};

// Order commands by time in expanded sequences.
function sortCommandsByTime(commands: CommandStem<{} | []>[]): {
  sortedCommands: CommandStem<{} | []>[];
  timeSorted: boolean;
} {
  // Holds the relative times converted into absolute times.
  const relativeTimeTracker: Record<string, Temporal.Instant> = {};

  let previousTime: Temporal.Instant | null = null;

  // If we have mixed time commands then we only sort if they're all absolute and relative.
  for (const command of commands) {
    // If the command is epoch-relative, complete, or the first command and relative then short circuit and don't try and sort.
    if (
      command.epochTime !== null ||
      (command.absoluteTime === null && command.epochTime === null && command.relativeTime === null) ||
      (command.relativeTime !== null && commands.indexOf(command) === 0)
    ) {
      return { sortedCommands: commands, timeSorted: false };
    }

    if (command.relativeTime !== null && previousTime !== null) {
      relativeTimeTracker[command.stem + command.relativeTime?.toString()] = previousTime.add(command.relativeTime);

      previousTime = previousTime.add(command.relativeTime);
    } else {
      previousTime = command.absoluteTime;
    }
  }

  // The commands will either be absolute or relative at this point, so we just compare their times.
  commands.sort((a, b) => {
    const firstCommandTime = a.absoluteTime ?? relativeTimeTracker[a.stem + a.relativeTime?.toString()];
    const secondCommandTime = b.absoluteTime ?? relativeTimeTracker[b.stem + b.relativeTime?.toString()];

    if (firstCommandTime !== undefined && secondCommandTime !== undefined) {
      return Temporal.Instant.compare(firstCommandTime, secondCommandTime);
    }

    return 0;
  });

  return { sortedCommands: commands, timeSorted: true };
}
