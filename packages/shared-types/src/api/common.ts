export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

export interface DateRangeParams {
  startDate: string;
  endDate: string;
}

export interface CommonQueryParams extends PageParams, DateRangeParams {
  vendorId?: string;
  borough?: string;
  taxiType?: string;
}

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

export interface PageResponse<T = unknown> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export enum DateRangePreset {
  Q1_2025 = 'Q1_2025',
  LAST_7_DAYS = 'LAST_7_DAYS',
  LAST_30_DAYS = 'LAST_30_DAYS',
  CUSTOM = 'CUSTOM'
}

export interface ChartConfig {
  chartType: ChartType;
  xAxisField: string;
  yAxisField: string;
  title: string;
  data: Record<string, unknown>[];
}

export type ChartType = 'bar' | 'line' | 'pie' | 'scatter' | 'heatmap' | 'table';
