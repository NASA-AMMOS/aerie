export type ErrorLike = {
  message: string,
  name: string,
  stack?: string | null,
  [key: string]: any,
}

export class InheritedError extends Error {
  public readonly cause: ErrorLike | ErrorLike[];

  constructor(message: string, cause: ErrorLike | ErrorLike[]) {
    let inheritedMessage = message + indent(
        '\nCaused by: ' + (Array.isArray(cause)
        ? '[\n' + indent(cause.map(c => c.message).join(',\n')) + '\n]'
        : cause.message)
    );
    super(inheritedMessage);
    this.cause = cause;
    this.stack = (this.stack?.replaceAll('    ', '\t\t') ?? this.message)  + indent(
        '\nCaused by: ' + (Array.isArray(cause)
        ? '[\n' + indent(cause.map(c => c.stack?.replaceAll('    ', '\t\t') ?? c.message).join(',\n')) + '\n]'
        : (cause.stack?.replaceAll('    ', '\t\t') ?? cause.message))
    );
  }
}

function indent(str: string, times: number = 1) {
  return (str.startsWith('\n') ? '' : '\t') + str.replaceAll('\n', '\n' + '\t'.repeat(times));
}
