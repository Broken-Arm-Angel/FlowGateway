package com.dpeng.gateway.config.util;

import com.dpeng.gateway.config.loader.ConfigLoader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;


/**
 * 通过jackson从yaml配置文件中拿到数据
 */
public class ConfigUtil {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T loadConfigFromYaml(String filePath, Class<T> clazz, String prefix) {
        //调用类加载器加载文件流
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) return null;
            ObjectNode rootNode = (ObjectNode) mapper.readTree(inputStream);
            ObjectNode subNode = getSubNode(rootNode, prefix);
            return mapper.treeToValue(subNode, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectNode getSubNode(ObjectNode node, String prefix) {
        if (prefix == null || prefix.isEmpty()) return node;
        String[] keys = prefix.split("\\.");
        for (String key : keys) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            node = (ObjectNode) node.get(key);
        }
        return node;
    }

}
