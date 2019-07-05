import { ChartPoint } from "chart.js";

export interface KafkaPoint extends ChartPoint {
  x: number;
  y: number;
}

export type KafkaEvent<T> = {
  headers: {
    headers: any[];
    isReadOnly: boolean;
  };
  key: string;
  offset: number;
  partition: number;
  serializedKeySize: number;
  serializedValueSize: number;
  timestamp: number;
  timestampType: string;
  topic: string;
  value: T;
};

export type KafkaPointEvent = KafkaEvent<KafkaPoint>;
