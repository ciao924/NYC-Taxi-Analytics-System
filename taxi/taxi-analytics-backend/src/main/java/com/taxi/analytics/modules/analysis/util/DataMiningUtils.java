package com.taxi.analytics.modules.analysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class DataMiningUtils {

    private static final Logger log = LoggerFactory.getLogger(DataMiningUtils.class);

    private DataMiningUtils() {
    }

    /**
     * 计算加权移动平均（WMA）
     */
    public static List<Double> calculateWeightedMovingAverage(List<Double> values, int windowSize) {
        if (values == null || values.isEmpty() || windowSize < 1) {
            return Collections.emptyList();
        }
        
        List<Double> wma = new ArrayList<>();
        
        for (int i = 0; i < values.size(); i++) {
            int actualWindow = Math.min(i + 1, windowSize);
            double weightedSum = 0.0;
            double weightSum = 0.0;
            
            for (int j = 0; j < actualWindow; j++) {
                double weight = actualWindow - j;
                weightedSum += values.get(i - j) * weight;
                weightSum += weight;
            }
            
            wma.add(weightedSum / weightSum);
        }
        
        return wma;
    }

    /**
     * 计算相对强弱指数（RSI）
     */
    public static List<Double> calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() < period + 1) {
            return Collections.emptyList();
        }
        
        List<Double> rsi = new ArrayList<>();
        
        for (int i = 0; i < period; i++) {
            rsi.add(50.0);
        }
        
        double avgGain = 0.0;
        double avgLoss = 0.0;
        
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i) - prices.get(i - 1);
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }
        
        avgGain /= period;
        avgLoss /= period;
        
        for (int i = period; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;
            
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            
            double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsi.add(Math.min(100, Math.max(0, 100 - (100 / (1 + rs)))));
        }
        
        return rsi;
    }

    /**
     * 计算变异系数（CV）- 衡量数据离散程度
     */
    public static double calculateCoefficientOfVariation(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        return mean == 0 ? 0.0 : stdDev / mean;
    }

    /**
     * 检测序列中的趋势变化点（使用滑动窗口比较）
     */
    public static List<Integer> detectTrendChangePoints(List<Double> values, int windowSize, double threshold) {
        if (values == null || values.size() < windowSize * 2) {
            return Collections.emptyList();
        }
        
        List<Integer> changePoints = new ArrayList<>();
        
        for (int i = windowSize; i < values.size() - windowSize; i++) {
            List<Double> prevWindow = values.subList(i - windowSize, i);
            List<Double> currWindow = values.subList(i, i + windowSize);
            
            double prevMean = calculateMean(prevWindow);
            double currMean = calculateMean(currWindow);
            
            double diff = Math.abs(currMean - prevMean);
            double avgMean = (prevMean + currMean) / 2;
            
            if (avgMean != 0 && diff / avgMean > threshold) {
                changePoints.add(i);
            }
        }
        
        return changePoints;
    }

    /**
     * 计算时间序列的周期性特征
     */
    public static Map<String, Object> analyzeSeasonality(List<Double> values, int period) {
        Map<String, Object> result = new HashMap<>();
        
        if (values == null || values.isEmpty()) {
            result.put("seasonal", false);
            result.put("strength", 0.0);
            return result;
        }
        
        int n = values.size();
        if (n < period * 2) {
            result.put("seasonal", false);
            result.put("strength", 0.0);
            return result;
        }
        
        double totalVariance = calculateVariance(values);
        
        List<Double> seasonalMeans = new ArrayList<>();
        for (int i = 0; i < period; i++) {
            double sum = 0.0;
            int count = 0;
            for (int j = i; j < n; j += period) {
                sum += values.get(j);
                count++;
            }
            seasonalMeans.add(count > 0 ? sum / count : 0.0);
        }
        
        double seasonalVariance = calculateVariance(seasonalMeans);
        double strength = totalVariance > 0 ? seasonalVariance / totalVariance : 0.0;
        
        result.put("seasonal", strength > 0.3);
        result.put("strength", strength);
        result.put("period", period);
        result.put("seasonalMeans", seasonalMeans);
        
        return result;
    }

    /**
     * 计算两个序列之间的互相关
     */
    public static double calculateCrossCorrelation(List<Double> x, List<Double> y, int lag) {
        if (x == null || y == null || x.size() != y.size() || x.size() < Math.abs(lag) + 2) {
            return 0.0;
        }
        
        int n = x.size();
        int effectiveN = n - Math.abs(lag);
        
        double meanX = calculateMean(x);
        double meanY = calculateMean(y);
        
        double numerator = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;
        
        for (int i = 0; i < effectiveN; i++) {
            int j = i + lag;
            if (j >= 0 && j < n) {
                numerator += (x.get(i) - meanX) * (y.get(j) - meanY);
                denomX += Math.pow(x.get(i) - meanX, 2);
                denomY += Math.pow(y.get(j) - meanY, 2);
            }
        }
        
        double denominator = Math.sqrt(denomX * denomY);
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    /**
     * 简单指数平滑预测
     */
    public static List<Double> exponentialSmoothingForecast(List<Double> values, double alpha, int forecastPeriods) {
        if (values == null || values.isEmpty() || forecastPeriods <= 0) {
            return Collections.emptyList();
        }
        
        List<Double> smoothed = new ArrayList<>();
        smoothed.add(values.get(0));
        
        for (int i = 1; i < values.size(); i++) {
            smoothed.add(alpha * values.get(i) + (1 - alpha) * smoothed.get(i - 1));
        }
        
        List<Double> forecast = new ArrayList<>();
        double lastSmoothed = smoothed.get(smoothed.size() - 1);
        
        for (int i = 0; i < forecastPeriods; i++) {
            forecast.add(lastSmoothed);
        }
        
        return forecast;
    }

    /**
     * 霍尔特线性趋势预测
     */
    public static List<Double> holtLinearForecast(List<Double> values, double alpha, double beta, int forecastPeriods) {
        if (values == null || values.isEmpty() || values.size() < 2 || forecastPeriods <= 0) {
            return Collections.emptyList();
        }
        
        int n = values.size();
        double[] level = new double[n];
        double[] trend = new double[n];
        
        level[0] = values.get(0);
        trend[0] = values.get(1) - values.get(0);
        
        for (int i = 1; i < n; i++) {
            level[i] = alpha * values.get(i) + (1 - alpha) * (level[i - 1] + trend[i - 1]);
            trend[i] = beta * (level[i] - level[i - 1]) + (1 - beta) * trend[i - 1];
        }
        
        List<Double> forecast = new ArrayList<>();
        for (int i = 1; i <= forecastPeriods; i++) {
            forecast.add(Math.round((level[n - 1] + i * trend[n - 1]) * 100) / 100.0);
        }
        
        return forecast;
    }

    /**
     * 计算Gini系数（衡量分布不均程度）
     */
    public static double calculateGiniCoefficient(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int n = sorted.size();
        
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += (i + 1) * sorted.get(i);
        }
        
        double total = sorted.stream().mapToDouble(Double::doubleValue).sum();
        
        return total > 0 ? (2.0 * sum) / (n * total) - (n + 1.0) / n : 0.0;
    }

    /**
     * 检测数据中的模式（上升/下降趋势）
     */
    public static String detectPattern(List<Double> values) {
        if (values == null || values.size() < 3) {
            return "stable";
        }
        
        double slope = calculateLinearTrendSlope(values);
        
        if (slope > 0.01) {
            return "increasing";
        } else if (slope < -0.01) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    /**
     * 计算线性趋势斜率
     */
    private static double calculateLinearTrendSlope(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        
        int n = values.size();
        List<Double> xValues = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            xValues.add((double) i);
        }
        
        LinearRegressionResult regression = performLinearRegression(xValues, values);
        return regression.getSlope();
    }

    /**
     * 计算熵值（衡量数据分布的不确定性）
     */
    public static double calculateEntropy(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        Map<Double, Long> frequency = values.stream()
            .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        
        int total = values.size();
        double entropy = 0.0;
        
        for (Long count : frequency.values()) {
            double probability = (double) count / total;
            entropy -= probability * Math.log(probability) / Math.log(2);
        }
        
        return entropy;
    }

    /**
     * 计算偏度（衡量数据分布的不对称性）
     */
    public static double calculateSkewness(List<Double> values) {
        if (values == null || values.size() < 3) {
            return 0.0;
        }
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) {
            return 0.0;
        }
        
        int n = values.size();
        double sum = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 3))
            .sum();
        
        return (n * sum) / ((n - 1) * (n - 2) * Math.pow(stdDev, 3));
    }

    /**
     * 计算峰度（衡量数据分布的尖锐程度）
     */
    public static double calculateKurtosis(List<Double> values) {
        if (values == null || values.size() < 4) {
            return 0.0;
        }
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) {
            return 0.0;
        }
        
        int n = values.size();
        double sum = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 4))
            .sum();
        
        double variance = Math.pow(stdDev, 2);
        double kurtosis = (n * (n + 1) * sum) / ((n - 1) * (n - 2) * (n - 3) * Math.pow(variance, 2));
        
        return kurtosis - 3.0; // 减去3使其以正态分布为基准
    }

    public static double calculateMean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    public static double calculateVariance(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double mean = calculateMean(values);
        return values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
    }

    public static double calculateStandardDeviation(List<Double> values) {
        return Math.sqrt(calculateVariance(values));
    }

    public static List<Double> calculateZScore(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) {
            return values.stream().map(v -> 0.0).collect(Collectors.toList());
        }
        
        return values.stream()
            .map(v -> (v - mean) / stdDev)
            .collect(Collectors.toList());
    }

    public static Map<Integer, AnomalyResult> detectAnomaliesByZScore(List<Double> values, double threshold) {
        Map<Integer, AnomalyResult> anomalies = new HashMap<>();
        List<Double> zScores = calculateZScore(values);
        
        for (int i = 0; i < zScores.size(); i++) {
            double zScore = Math.abs(zScores.get(i));
            if (zScore > threshold) {
                anomalies.put(i, new AnomalyResult(i, values.get(i), zScore, 
                    zScore > threshold * 1.3 ? "critical" : zScore > threshold * 1.1 ? "high" : "medium"));
            }
        }
        
        return anomalies;
    }

    public static LinearRegressionResult performLinearRegression(List<Double> xValues, List<Double> yValues) {
        if (xValues == null || yValues == null || xValues.size() != yValues.size() || xValues.size() < 2) {
            return new LinearRegressionResult(0.0, 0.0, 0.0);
        }
        
        int n = xValues.size();
        double sumX = xValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumY = yValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumX2 = 0.0;
        
        for (int i = 0; i < n; i++) {
            sumXY += xValues.get(i) * yValues.get(i);
            sumX2 += Math.pow(xValues.get(i), 2);
        }
        
        double denominator = n * sumX2 - Math.pow(sumX, 2);
        if (denominator == 0) {
            return new LinearRegressionResult(0.0, 0.0, 0.0);
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        double ssTot = 0.0;
        double ssRes = 0.0;
        double meanY = sumY / n;
        
        for (int i = 0; i < n; i++) {
            ssTot += Math.pow(yValues.get(i) - meanY, 2);
            ssRes += Math.pow(yValues.get(i) - (slope * xValues.get(i) + intercept), 2);
        }
        
        double rSquared = ssTot == 0 ? 1.0 : 1 - (ssRes / ssTot);
        
        return new LinearRegressionResult(slope, intercept, rSquared);
    }

    public static List<Double> calculateMovingAverage(List<Double> values, int windowSize) {
        if (values == null || values.isEmpty() || windowSize < 1) {
            return Collections.emptyList();
        }
        
        List<Double> movingAverages = new ArrayList<>();
        double windowSum = 0.0;
        int count = 0;
        
        for (int i = 0; i < values.size(); i++) {
            windowSum += values.get(i);
            count++;
            
            if (count >= windowSize) {
                movingAverages.add(windowSum / windowSize);
                windowSum -= values.get(i - windowSize + 1);
                count--;
            } else {
                movingAverages.add(windowSum / count);
            }
        }
        
        return movingAverages;
    }

    public static List<Double> calculateExponentialMovingAverage(List<Double> values, double alpha) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Double> ema = new ArrayList<>();
        ema.add(values.get(0));
        
        for (int i = 1; i < values.size(); i++) {
            double currentEma = alpha * values.get(i) + (1 - alpha) * ema.get(i - 1);
            ema.add(currentEma);
        }
        
        return ema;
    }

    public static List<Double> predictFutureValues(List<Double> historicalValues, int periods, double alpha) {
        if (historicalValues == null || historicalValues.isEmpty() || periods <= 0) {
            return Collections.emptyList();
        }
        
        List<Double> predictions = new ArrayList<>();
        List<Double> ema = calculateExponentialMovingAverage(historicalValues, alpha);
        
        double lastEma = ema.get(ema.size() - 1);
        double trend = 0.0;
        
        if (ema.size() >= 2) {
            trend = lastEma - ema.get(ema.size() - 2);
        }
        
        for (int i = 1; i <= periods; i++) {
            predictions.add(Math.round((lastEma + trend * i) * 100) / 100.0);
        }
        
        return predictions;
    }

    public static double calculateCorrelation(List<Double> xValues, List<Double> yValues) {
        if (xValues == null || yValues == null || xValues.size() != yValues.size() || xValues.size() < 2) {
            return 0.0;
        }
        
        int n = xValues.size();
        double sumX = xValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumY = yValues.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        
        for (int i = 0; i < n; i++) {
            sumXY += xValues.get(i) * yValues.get(i);
            sumX2 += Math.pow(xValues.get(i), 2);
            sumY2 += Math.pow(yValues.get(i), 2);
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumX2 - Math.pow(sumX, 2)) * (n * sumY2 - Math.pow(sumY, 2)));
        
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    public static <T> Map<T, Long> calculateFrequencyDistribution(List<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return values.stream()
            .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
    }

    public static <T> T findMode(List<T> values) {
        Map<T, Long> frequency = calculateFrequencyDistribution(values);
        return frequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public static double calculateMedian(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    public static Map<String, Double> calculateQuartiles(List<Double> values) {
        Map<String, Double> quartiles = new HashMap<>();
        
        if (values == null || values.isEmpty()) {
            quartiles.put("q1", 0.0);
            quartiles.put("q2", 0.0);
            quartiles.put("q3", 0.0);
            quartiles.put("iqr", 0.0);
            return quartiles;
        }
        
        List<Double> sorted = values.stream().sorted().collect(Collectors.toList());
        int size = sorted.size();
        
        int q1Index = size / 4;
        int q2Index = size / 2;
        int q3Index = (size * 3) / 4;
        
        quartiles.put("q1", sorted.get(q1Index));
        quartiles.put("q2", sorted.get(q2Index));
        quartiles.put("q3", sorted.get(q3Index));
        quartiles.put("iqr", quartiles.get("q3") - quartiles.get("q1"));
        
        return quartiles;
    }

    public static List<Double> detectOutliersUsingIQR(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Double> quartiles = calculateQuartiles(values);
        double iqr = quartiles.get("iqr");
        double q1 = quartiles.get("q1");
        double q3 = quartiles.get("q3");
        
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        return values.stream()
            .filter(v -> v < lowerBound || v > upperBound)
            .collect(Collectors.toList());
    }

    public static <T> List<Cluster<T>> kMeansClustering(List<DataPoint<T>> dataPoints, int k, int maxIterations) {
        if (dataPoints == null || dataPoints.isEmpty() || k < 1) {
            return Collections.emptyList();
        }
        
        int n = dataPoints.size();
        k = Math.min(k, n);
        
        List<DataPoint<T>> centroids = initializeCentroids(dataPoints, k);
        List<Cluster<T>> clusters = new ArrayList<>();
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            clusters = assignPointsToClusters(dataPoints, centroids);
            List<DataPoint<T>> newCentroids = recalculateCentroids(clusters);
            
            if (centroidsEqual(centroids, newCentroids)) {
                log.debug("K-means converged after {} iterations", iteration + 1);
                break;
            }
            
            centroids = newCentroids;
        }
        
        return clusters;
    }

    private static <T> List<DataPoint<T>> initializeCentroids(List<DataPoint<T>> dataPoints, int k) {
        List<DataPoint<T>> centroids = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < k; i++) {
            int index = random.nextInt(dataPoints.size());
            DataPoint<T> point = dataPoints.get(index);
            centroids.add(new DataPoint<>(null, point.getFeatures()));
        }
        
        return centroids;
    }

    private static <T> List<Cluster<T>> assignPointsToClusters(List<DataPoint<T>> dataPoints, List<DataPoint<T>> centroids) {
        List<Cluster<T>> clusters = new ArrayList<>();
        for (int i = 0; i < centroids.size(); i++) {
            clusters.add(new Cluster<>(i));
        }
        
        for (DataPoint<T> point : dataPoints) {
            double minDistance = Double.MAX_VALUE;
            int closestCluster = 0;
            
            for (int i = 0; i < centroids.size(); i++) {
                double distance = euclideanDistance(point.getFeatures(), centroids.get(i).getFeatures());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCluster = i;
                }
            }
            
            clusters.get(closestCluster).addPoint(point);
        }
        
        return clusters;
    }

    private static <T> List<DataPoint<T>> recalculateCentroids(List<Cluster<T>> clusters) {
        List<DataPoint<T>> centroids = new ArrayList<>();
        
        for (Cluster<T> cluster : clusters) {
            List<DataPoint<T>> points = cluster.getPoints();
            if (points.isEmpty()) {
                continue;
            }
            
            int numFeatures = points.get(0).getFeatures().length;
            double[] newCentroid = new double[numFeatures];
            
            for (DataPoint<T> point : points) {
                double[] features = point.getFeatures();
                for (int i = 0; i < numFeatures; i++) {
                    newCentroid[i] += features[i];
                }
            }
            
            for (int i = 0; i < numFeatures; i++) {
                newCentroid[i] /= points.size();
            }
            
            centroids.add(new DataPoint<>(null, newCentroid));
        }
        
        return centroids;
    }

    private static <T> boolean centroidsEqual(List<DataPoint<T>> centroids1, List<DataPoint<T>> centroids2) {
        if (centroids1.size() != centroids2.size()) {
            return false;
        }
        
        for (int i = 0; i < centroids1.size(); i++) {
            double[] f1 = centroids1.get(i).getFeatures();
            double[] f2 = centroids2.get(i).getFeatures();
            
            if (!Arrays.equals(f1, f2)) {
                return false;
            }
        }
        
        return true;
    }

    public static double euclideanDistance(double[] point1, double[] point2) {
        if (point1 == null || point2 == null || point1.length != point2.length) {
            return Double.MAX_VALUE;
        }
        
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        
        return Math.sqrt(sum);
    }

    public static double calculateSeasonalIndex(List<Double> values, int period) {
        if (values == null || values.isEmpty() || period < 2) {
            return 0.0;
        }
        
        int numPeriods = values.size() / period;
        if (numPeriods < 2) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            double periodSum = 0.0;
            for (int j = 0; j < numPeriods; j++) {
                int index = j * period + i;
                if (index < values.size()) {
                    periodSum += values.get(index);
                }
            }
            sum += periodSum / numPeriods;
        }
        
        return sum / period;
    }

    public static List<Double> decomposeTimeSeries(List<Double> values, int period) {
        if (values == null || values.isEmpty() || period < 2) {
            return Collections.emptyList();
        }
        
        List<Double> movingAverages = calculateMovingAverage(values, period);
        List<Double> seasonalComponents = new ArrayList<>();
        
        for (int i = 0; i < values.size() && i < movingAverages.size(); i++) {
            if (movingAverages.get(i) != 0) {
                seasonalComponents.add(values.get(i) / movingAverages.get(i));
            } else {
                seasonalComponents.add(1.0);
            }
        }
        
        return seasonalComponents;
    }

    public static class AnomalyResult {
        private final int index;
        private final double value;
        private final double zScore;
        private final String severity;

        public AnomalyResult(int index, double value, double zScore, String severity) {
            this.index = index;
            this.value = value;
            this.zScore = zScore;
            this.severity = severity;
        }

        public int getIndex() {
            return index;
        }

        public double getValue() {
            return value;
        }

        public double getZScore() {
            return zScore;
        }

        public String getSeverity() {
            return severity;
        }
    }

    public static class LinearRegressionResult {
        private final double slope;
        private final double intercept;
        private final double rSquared;

        public LinearRegressionResult(double slope, double intercept, double rSquared) {
            this.slope = slope;
            this.intercept = intercept;
            this.rSquared = rSquared;
        }

        public double getSlope() {
            return slope;
        }

        public double getIntercept() {
            return intercept;
        }

        public double getRSquared() {
            return rSquared;
        }
    }

    public static class DataPoint<T> {
        private final T label;
        private final double[] features;

        public DataPoint(T label, double[] features) {
            this.label = label;
            this.features = features;
        }

        public T getLabel() {
            return label;
        }

        public double[] getFeatures() {
            return features;
        }
    }

    public static class Cluster<T> {
        private final int id;
        private final List<DataPoint<T>> points;

        public Cluster(int id) {
            this.id = id;
            this.points = new ArrayList<>();
        }

        public int getId() {
            return id;
        }

        public List<DataPoint<T>> getPoints() {
            return points;
        }

        public void addPoint(DataPoint<T> point) {
            points.add(point);
        }

        public double[] getCentroid() {
            if (points.isEmpty()) {
                return new double[0];
            }
            
            int numFeatures = points.get(0).getFeatures().length;
            double[] centroid = new double[numFeatures];
            
            for (DataPoint<T> point : points) {
                double[] features = point.getFeatures();
                for (int i = 0; i < numFeatures; i++) {
                    centroid[i] += features[i];
                }
            }
            
            for (int i = 0; i < numFeatures; i++) {
                centroid[i] /= points.size();
            }
            
            return centroid;
        }
    }
}