package com.lumix.infrastructure.redis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis standalone / cluster 的共用設定。
 *
 * <p>mode 只決定 client topology，不會自動把單點資料複製成 cluster。cluster 節點與密碼須由部署
 * 環境注入，不能放進版本控制。</p>
 */
@ConfigurationProperties("lumix.redis")
public class RedisTopologyProperties {

    private Mode mode = Mode.STANDALONE;
    private String host;
    private int port = 6379;
    private String password;
    private List<String> clusterNodes = new ArrayList<>();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.STANDALONE : mode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getClusterNodes() {
        return List.copyOf(clusterNodes);
    }

    public void setClusterNodes(List<String> clusterNodes) {
        this.clusterNodes = clusterNodes == null ? new ArrayList<>() : new ArrayList<>(clusterNodes);
    }

    public enum Mode {
        STANDALONE,
        CLUSTER
    }
}
