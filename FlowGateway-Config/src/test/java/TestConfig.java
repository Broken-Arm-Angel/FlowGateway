import com.dpeng.gateway.config.config.Config;
import com.dpeng.gateway.config.service.ConfigCenterProcessor;
import com.dpeng.gateway.config.service.impl.nacos.NacosConfigCenter;
import com.dpeng.gateway.config.util.ConfigUtil;
import org.junit.Before;
import org.junit.Test;

import static com.dpeng.gateway.common.constant.ConfigConstant.CONFIG_PATH;
import static com.dpeng.gateway.common.constant.ConfigConstant.CONFIG_PREFIX;

public class TestConfig {

    Config config;

    @Before
    public void before() {
        this.config = ConfigUtil.loadConfigFromYaml(CONFIG_PATH, Config.class, CONFIG_PREFIX);
    }

    @Test
    public void testConfigLoad() {
        System.out.println(config);
    }

    @Test
    public void testNacosConfig() {
        ConfigCenterProcessor processor = new NacosConfigCenter();
        processor.init(config.getConfigCenter());
        processor.subscribeRoutesChange(i -> {});
    }
}