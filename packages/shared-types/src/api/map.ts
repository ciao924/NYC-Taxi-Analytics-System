import { DateRangeParams } from './common';

export interface HeatmapData {
  pickupZone: string;
  dropoffZone: string;
  tripCount: number;
  avgFare: number;
  latitude: number;
  longitude: number;
}

export interface HotspotData {
  zoneId: string;
  zoneName: string;
  tripCount: number;
  rank: number;
  type: 'pickup' | 'dropoff';
  latitude: number;
  longitude: number;
}

export interface ZoneInfo {
  zoneId: string;
  zoneName: string;
  borough: string;
  latitude: number;
  longitude: number;
}

export interface FlowData {
  originZone: string;
  destZone: string;
  tripCount: number;
  avgDuration: number;
  avgFare: number;
}

export interface HeatmapQueryRequest extends DateRangeParams {
  type?: 'pickup' | 'dropoff';
  limit?: number;
}

export interface HotspotQueryRequest extends DateRangeParams {
  type: 'pickup' | 'dropoff';
  limit?: number;
}
