package com.dpeng.gateway.core.algorithm;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 负载均衡: 一致性hash类
 */
public class ConsistentHashing {

    private final int virtualNodeNum;// 每个物理节点对应的虚拟节点数

    // 哈希环（key: 哈希值, value: 真实节点名称）
    private final SortedMap<Integer, String> hashCircle = new TreeMap<>();

    /**
     * 构造函数，初始化一致性哈希环
     * @param nodes 网关上游的微服务实例
     * @param virtualNodeNum 每个物理节点对应虚拟节点的个数
     */
    public ConsistentHashing(List<String> nodes, int virtualNodeNum) {
        this.virtualNodeNum = virtualNodeNum;
        for (String node : nodes) {
            addNode(node);
        }
    }

    public void addNode(String node) {
        for (int i = 0; i < virtualNodeNum; i++) {
            String virtualNode = node + "&&VN" + i;
            hashCircle.put(getHash(virtualNode), node);
        }
    }

    //当一个客户端请求带有 key（如缓存 Key），会通过一致性哈希找到对应的存储节点。
    public String getNode(String key) {
        if (hashCircle.isEmpty()) {
            return null;
        }
        int hash = getHash(key);
        SortedMap<Integer, String> tailMap = hashCircle.tailMap(hash);// 获取 >= hash 的子映射
        Integer nodeHash = tailMap.isEmpty() ? hashCircle.firstKey() : tailMap.firstKey();// 取大于等于当前 hash 的第一个节点, 注意这里是hash环
        return hashCircle.get(nodeHash);
    }

    //采用 FNV-1a 哈希算法，这是一种快速且分布均匀的哈希算法，用于计算字符串的哈希值。
    private int getHash(String str) {
        final int p = 16777619;// 质数因子
        int hash = (int) 2166136261L;// FNV 初始值
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;// FNV-1a 核心公式
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        if (hash < 0) {
            hash = Math.abs(hash);
        }
        return hash;
    }

}