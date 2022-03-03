export type ErrorLike = {
  message: string,
  name: string,
  stack?: string | null,
  [key: string]: any,
}

export class InheritedError extends Error {
  readonly #cause?: ErrorLike;

  constructor(message?: string, cause?: ErrorLike) {
    super(message);
    this.#cause = cause;
    if (this.#cause !== undefined) {
      this.stack = `${this.stack}\n${this.#cause ? ('\tInherited From: \n\t' + this.#cause.stack ?? this.#cause.message).replace(/\n/g, '\n\t') : ''}\n`;
    }
  }
}
