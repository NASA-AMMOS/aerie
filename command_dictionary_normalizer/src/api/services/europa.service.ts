import Promise from "bluebird";
import L from "../../common/logger";
import { europaConverter } from "../converters/europa.converter";
import { EuropaDictionaryInput } from "../types/europa.type";

export class EuropaService {
  convert(xml: EuropaDictionaryInput): Promise<any> {
    L.info(`Received Europa Command Dictionary`);

    const jsonData = europaConverter(xml);

    return Promise.resolve(jsonData);
  }
}

export default new EuropaService();
