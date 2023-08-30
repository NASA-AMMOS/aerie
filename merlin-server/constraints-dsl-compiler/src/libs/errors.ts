export class DirtyGeneratorError extends Error {
  private static message: string = "A generator was reused without being cloned. See Profile.clone documentation for explanation.";
  constructor() {
    super(DirtyGeneratorError.message);
    this.name = "DirtyGeneratorError";
  }
}
