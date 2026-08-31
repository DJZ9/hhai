package tech.aiflowy.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OCR 异步任务队列配置（Redis 持久化）。
 */
@ConfigurationProperties(prefix = "aiflowy.async-task")
public class AsyncTaskProperties {

    private boolean enabled = true;
    private long trackingTimeoutMillis = 1_800_000L;
    private Redis redis = new Redis();
    private Worker worker = new Worker();
    private Retry retry = new Retry();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getTrackingTimeoutMillis() {
        return trackingTimeoutMillis;
    }

    public void setTrackingTimeoutMillis(long trackingTimeoutMillis) {
        this.trackingTimeoutMillis = trackingTimeoutMillis;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public static class Redis {
        private String uri = "redis://127.0.0.1:6379/0";
        private String keyPrefix = "aiflowy:ocr-task:";

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Worker {
        private long scanIntervalMillis = 1000L;
        private int batchSize = 10;
        private long leaseMillis = 30000L;

        public long getScanIntervalMillis() {
            return scanIntervalMillis;
        }

        public void setScanIntervalMillis(long scanIntervalMillis) {
            this.scanIntervalMillis = scanIntervalMillis;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getLeaseMillis() {
            return leaseMillis;
        }

        public void setLeaseMillis(long leaseMillis) {
            this.leaseMillis = leaseMillis;
        }
    }

    public static class Retry {
        private long queryIntervalMillis = 3000L;
        private long initialErrorDelayMillis = 1000L;
        private long maxErrorDelayMillis = 60000L;
        private int maxConsecutiveErrors = 5;

        public long getQueryIntervalMillis() {
            return queryIntervalMillis;
        }

        public void setQueryIntervalMillis(long queryIntervalMillis) {
            this.queryIntervalMillis = queryIntervalMillis;
        }

        public long getInitialErrorDelayMillis() {
            return initialErrorDelayMillis;
        }

        public void setInitialErrorDelayMillis(long initialErrorDelayMillis) {
            this.initialErrorDelayMillis = initialErrorDelayMillis;
        }

        public long getMaxErrorDelayMillis() {
            return maxErrorDelayMillis;
        }

        public void setMaxErrorDelayMillis(long maxErrorDelayMillis) {
            this.maxErrorDelayMillis = maxErrorDelayMillis;
        }

        public int getMaxConsecutiveErrors() {
            return maxConsecutiveErrors;
        }

        public void setMaxConsecutiveErrors(int maxConsecutiveErrors) {
            this.maxConsecutiveErrors = maxConsecutiveErrors;
        }
    }
}
