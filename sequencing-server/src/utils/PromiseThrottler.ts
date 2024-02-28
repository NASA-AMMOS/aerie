export class PromiseThrottler {
  private runningPromises: Promise<unknown>[] = [];
  private promiseLimit: number;

  public constructor(promiseLimit: number) {
    this.promiseLimit = promiseLimit;
  }

  public async run<T>(promiseFactory: () => Promise<T>): Promise<T> {
    while (this.runningPromises.length >= this.promiseLimit) {
      await Promise.race(this.runningPromises);
    }
    const promise = promiseFactory();
    this.runningPromises.push(promise);
    return promise.finally(() => {
      const index = this.runningPromises.indexOf(promise);
      if (index !== -1) {
        this.runningPromises.splice(index, 1);
      }
    });
  }
}
