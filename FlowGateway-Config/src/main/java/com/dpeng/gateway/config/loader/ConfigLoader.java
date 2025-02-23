package com.dpeng.gateway.config.loader;


import com.dpeng.gateway.config.config.Config;
import com.dpeng.gateway.config.util.ConfigUtil;
import static com.dpeng.gateway.common.constant.ConfigConstant.CONFIG_PATH;
import static com.dpeng.gateway.common.constant.ConfigConstant.CONFIG_PREFIX;

/**
 * 配置加载
 */
public class ConfigLoader {

    public static Config load(String[] args) {
        return ConfigUtil.loadConfigFromYaml(CONFIG_PATH, Config.class, CONFIG_PREFIX);
    }

}
