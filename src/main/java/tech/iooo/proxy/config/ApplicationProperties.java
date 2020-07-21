package tech.iooo.proxy.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 龙也
 * @date 2020/7/21 3:41 下午
 */
@Data
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private Integer port = 1081;
    private Boolean log = true;
    private Auth auth = new Auth();

    @Data
    public static class Auth {
        private String username;
        private String password;

        public boolean isAuth() {
            return StringUtils.isNoneBlank(username, password);
        }
    }
}
