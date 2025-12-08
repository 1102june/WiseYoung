package com.example.youth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 공공데이터/청년정책 API 호출용 WebClient Bean 정의.
 * PublicDataApiService 에서는 개별 API 특성에 맞춘 WebClient 를 @Qualifier 로 주입받는다.
 */
@Configuration
public class WebClientConfig {

    private static final ExchangeStrategies DEFAULT_STRATEGY = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(5 * 1024 * 1024)) // 5MB
            .build();

    @Bean
    @Qualifier("lhWebClient")
    public WebClient lhWebClient(
            WebClient.Builder builder,
            @Value("${public-data.lh.rental-house-list.url}") String baseUrl
    ) {
        return baseConfiguredClient(builder, baseUrl);
    }

    @Bean
    @Qualifier("lhRentalNoticeWebClient")
    public WebClient lhRentalNoticeWebClient(
            WebClient.Builder builder,
            @Value("${public-data.lh.rental-notice.url}") String baseUrl
    ) {
        return baseConfiguredClient(builder, baseUrl);
    }

    @Bean
    @Qualifier("youthPolicyWebClient")
    public WebClient youthPolicyWebClient(
            WebClient.Builder builder,
            @Value("${youth-policy.url}") String baseUrl
    ) {
        return baseConfiguredClient(builder, baseUrl);
    }

    private WebClient baseConfiguredClient(WebClient.Builder builder, String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(DEFAULT_STRATEGY)
                .build();
    }
}












