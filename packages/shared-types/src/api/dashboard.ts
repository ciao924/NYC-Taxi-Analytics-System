import { DateRangeParams, ChartConfig } from './common';

export interface KpiSummary {
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
  avgDistance: number;
}

export interface TrendData {
  statDate: string;
  totalTrips: number;
  totalRevenue: number;
  avgFare: number;
}

export interface DashboardQueryRequest extends DateRangeParams {}

export interface DashboardKpiResponse {
  kpi: KpiSummary;
  trend: TrendData[];
  chartConfig: ChartConfig;
}

export interface HourlyDistribution {
  hour: number;
  tripCount: number;
  avgFare: number;
}

export interface PaymentDistribution {
  paymentType: string;
  paymentTypeName: string;
  tripCount: number;
  percentage: number;
}

export interface VendorPerformance {
  vendorId: string;
  vendorName: string;
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
  rating: number;
}
