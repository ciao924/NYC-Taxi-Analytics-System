export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

export interface DateRangeParams {
  startDate?: string;
  endDate?: string;
}

export interface CommonQueryParams extends PageParams, DateRangeParams {
  vendorId?: string;
  borough?: string;
  taxiType?: string;
}

export enum DateRangePreset {
  Q1_2025 = 'Q1_2025',     // 2025-01-01 ~ 2025-03-31
  LAST_7_DAYS = 'LAST_7_DAYS',
  LAST_30_DAYS = 'LAST_30_DAYS',
  CUSTOM = 'CUSTOM'
}
