import Promise from "bluebird";
import L from "../../common/logger";

let id = 0;
interface Example {
  id: number;
  name: string;
}

const examples: Example[] = [
  { id: id++, name: "example 0" },
  { id: id++, name: "example 1" }
];

export class EuropaService {
  convert(name: string): Promise<Example> {
    L.info(`Received Europa Command Dictionary`);
    const example: Example = {
      id: id++,
      name
    };
    examples.push(example);
    return Promise.resolve(example);
  }
}

export default new EuropaService();
