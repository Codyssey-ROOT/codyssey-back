package site.defence.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudFrontUrlService {

    private final String baseUrl;

    public CloudFrontUrlService(
            @Value("${app.cdn.base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    public String createGetUrl(String objectKey) {
        return baseUrl + "/" + objectKey;
    }
}