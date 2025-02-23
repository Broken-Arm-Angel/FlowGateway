import com.dpeng.gateway.config.config.Config;
import com.dpeng.gateway.config.util.ConfigUtil;
import org.junit.Test;

public class TestConfig {

    @Test
    public void testConfigLoad() {
        Config config = ConfigUtil.loadConfigFromYaml("gateway.yaml", Config.class, "flow.gateway");
        System.out.println(config);
    }

}