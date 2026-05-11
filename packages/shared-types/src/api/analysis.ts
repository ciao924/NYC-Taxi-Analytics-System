import { DateRangeParams } from './common';

export interface AirportStatistics {
  airportCode: string;
  airportName: string;
  tripCount: number;
  avgFare: number;
  avgDistance: number;
  peakHour: number;
}

export interface AirportDetailedStatistics {
  airportCode: string;
  date: string;
  tripCount: number;
  totalFare: number;
  avgFare: number;
  passengerCount: number;
}

export interface VendorComparison {
  vendorId: string;
  vendorName: string;
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
  marketShare: number;
}

export interface VendorTrend {
  date: string;
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
}

export interface PaymentDistribution {
  paymentType: string;
  paymentTypeName: string;
  tripCount: number;
  totalAmount: number;
  percentage: number;
}

export interface PaymentTrend {
  date: string;
  cashCount: number;
  cardCount: number;
  cashAmount: number;
  cardAmount: number;
}

export interface DistanceDistribution {
  distanceRange: string;
  tripCount: number;
  percentage: number;
  avgFare: number;
}

export interface DurationDistribution {
  durationRange: string;
  tripCount: number;
  percentage: number;
  avgFare: number;
}

export interface HourlyAnalysis {
  hour: number;
  tripCount: number;
  avgFare: number;
  peakIndicator: boolean;
}

export interface AnalysisQueryRequest extends DateRangeParams {
  vendorId?: string;
  airportCode?: string;
}
