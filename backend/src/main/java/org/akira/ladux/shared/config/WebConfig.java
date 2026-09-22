package org.akira.ladux.shared.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.root:../uploads}")
    private String uploadRoot;
    // Chuẩn hóa URL: loại bỏ dấu / thừa ở cuối URL, ví dụ: /api/v1/products/ -> /api/v1/products
    // Nếu client gửi /api/v1/products/123/ -> /api/v1/products/123
    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashHandlerFilter() {
        UrlHandlerFilter filter = UrlHandlerFilter
                .trailingSlashHandler("/**")
                .wrapRequest()
                .build();
        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customizePageable() {
        return resolver -> {
            // Nếu client không truyền size, mặc định chỉ lấy 12 sản phẩm
            resolver.setFallbackPageable(PageRequest.of(0, 12));

            // CHỐT CHẶN TỬ HUYỆT: Dù client truyền size to thế nào, tối đa cũng chỉ được lấy 50 dòng!
            resolver.setMaxPageSize(50);
        };
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = Path.of(uploadRoot)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
