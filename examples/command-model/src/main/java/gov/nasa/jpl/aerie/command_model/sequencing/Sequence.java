package gov.nasa.jpl.aerie.command_model.sequencing;

import gov.nasa.jpl.aerie.command_model.sequencing.Command.CommandResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record Sequence(String id, List<Command> commands) {
    public Optional<Command> getCommand(int index) {
        if (0 <= index && index < commands.size()) {
            return Optional.of(commands.get(index));
        } else {
            return Optional.empty();
        }
    }

    public static final class Builder {
        private final String id;
        private final List<Command> commands = new ArrayList<>();
        private final List<Block> openConditionals = new ArrayList<>();
        private final List<Block> openLoops = new ArrayList<>();

        public Builder(String id) {
            this.id = id;
        }

        /**
         * General command, control flows to next command when it finishes.
         */
        public Builder command(String stem, List<Object> arguments, Runnable action) {
            int nextCommandIndex = commands.size();
            commands.add(new Command(stem, arguments, () -> {
                action.run();
                return new CommandResult(nextCommandIndex);
            }));
            return this;
        }

        /**
         * Start a "conditional" style command block, like "if" or "switch".
         * The action returns which branch to take, starting at 0.
         * For an "if", branch 0 will be "then", and branch 1, "else".
         */
        public Builder startConditional(String stem, List<Object> arguments, Supplier<Integer> action) {
            var block = new Block();
            openConditionals.add(block);
            commands.add(new Command(stem, arguments, () -> new CommandResult(block.pointsOfInterest.get(action.get()))));
            return this;
        }

        /**
         * Add a branch to the topmost open conditional block.
         * Call this immediately before adding the command for "then" or "else" when creating "if" blocks,
         * or the "case" commands in a switch-case block.
         */
        public Builder branchConditional() {
            int nextCommandIndex = commands.size();
            var block = openConditionals.getLast();
            // Mark the next command to be added as the start of the next branch
            block.pointsOfInterest.add(nextCommandIndex);
            // Then replace the prior command with one that will skip to the end of the block when it completes.
            var priorCommand = commands.removeLast();
            commands.add(new Command(priorCommand.stem, priorCommand.arguments, () -> {
                var originalResult = priorCommand.run();
                if (originalResult.nextCommandIndex() == nextCommandIndex) {
                    // Instead of moving to this branch, skip to the end of the conditional block.
                    return new CommandResult(block.pointsOfInterest.getLast());
                } else {
                    return originalResult;
                }
            }));
            return this;
        }

        /**
         * Close the topmost open conditional block.
         * All branches will jump to the next command when they end.
         */
        public Builder endConditional() {
            int nextCommandIndex = commands.size();
            var block = openConditionals.removeLast();
            block.pointsOfInterest.add(nextCommandIndex);
            return this;
        }

        /**
         * Start a new loop block.
         * The action should return true if the loop runs and false if it ends.
         */
        public Builder startLoop(String stem, List<Object> arguments, Supplier<Boolean> action) {
            var block = new Block();
            openLoops.add(block);
            int nextCommandIndex = commands.size();
            block.pointsOfInterest.add(nextCommandIndex);
            commands.add(new Command(stem, arguments, () -> {
                boolean runLoop = action.get();
                return new CommandResult(runLoop ? nextCommandIndex : block.pointsOfInterest.getLast());
            }));
            return this;
        }

        /**
         * Add a "loop control" command to the topmost open loop.
         * The action should return one of three locations for control to go next:
         * the start of the loop, the end of the loop (i.e., exiting the loop), or the next command.
         * <p>This unifies both "continue" and "break" commands, including conditional and combined versions.</p>
         */
        public Builder controlLoop(String stem, List<Object> arguments, Supplier<LoopPosition> action) {
            var block = openLoops.getLast();
            int nextCommandIndex = commands.size();
            block.pointsOfInterest.add(nextCommandIndex);
            commands.add(new Command(stem, arguments, () -> {
                var loopPosition = action.get();
                return new CommandResult(switch (loopPosition) {
                    case START -> block.pointsOfInterest.getFirst();
                    case NEXT_COMMAND -> nextCommandIndex;
                    case END -> block.pointsOfInterest.getLast() + 1;
                });
            }));
            return this;
        }

        /**
         * Add a "continue" command to the topmost open loop.
         * Control flows back to the start of the loop after this command completes.
         */
        public Builder continueLoop(String stem, List<Object> arguments, Runnable action) {
            return controlLoop(stem, arguments, () -> {
                action.run();
                return LoopPosition.START;
            });
        }

        /**
         * Add a "break" command to the topmost open loop.
         * Control exits the loop after this command completes.
         */
        public Builder breakLoop(String stem, List<Object> arguments, Runnable action) {
            return controlLoop(stem, arguments, () -> {
                action.run();
                return LoopPosition.END;
            });
        }

        /**
         * Close the topmost open loop.
         * Control returns to the start of the loop after this command completes.
         */
        public Builder endLoop(String stem, List<Object> arguments, Runnable action) {
            var block = openLoops.removeLast();
            int nextCommandIndex = commands.size();
            block.pointsOfInterest.add(nextCommandIndex);
            commands.add(new Command(stem, arguments, () -> {
                action.run();
                return new CommandResult(block.pointsOfInterest.getFirst());
            }));
            return this;
        }

        public Sequence build() {
            if (!openConditionals.isEmpty()) {
                throw new IllegalStateException("Unclosed conditional in sequence!");
            }
            if (!openLoops.isEmpty()) {
                throw new IllegalStateException("Unclosed loop in sequence!");
            }
            return new Sequence(id, commands);
        }

        public enum LoopPosition {
            START,
            NEXT_COMMAND,
            END
        }
    }

    // This is an aid to Sequence.Builder, meant to help us create structured branching and looping.
    public static final class Block {
        private final List<Integer> pointsOfInterest = new ArrayList<>();
    }
}
