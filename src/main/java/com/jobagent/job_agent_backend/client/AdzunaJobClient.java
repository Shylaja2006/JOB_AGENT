package com.jobagent.job_agent_backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class AdzunaJobClient {

    private static final String ADZUNA_HOST = "https://api.adzuna.com";
    private static final int DEFAULT_RESULTS_PER_PAGE = 10;
    private static final int MAX_RESULTS_PER_PAGE = 50;

    private final RestClient restClient;
    private final String appId;
    private final String appKey;
    private final String country;

    public AdzunaJobClient(
            RestClient.Builder restClientBuilder,
            @Value("${adzuna.app-id}") String appId,
            @Value("${adzuna.app-key}") String appKey,
            @Value("${adzuna.country}") String country) {
        this.restClient = restClientBuilder.baseUrl(ADZUNA_HOST).build();
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
    }

    public List<AdzunaJob> search(String what, String where) {
        return search(what, where, DEFAULT_RESULTS_PER_PAGE);
    }

    public List<AdzunaJob> search(String what, String where, int resultsPerPage) {
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            return Collections.emptyList();
        }

        int pageSize = Math.min(Math.max(resultsPerPage, 1), MAX_RESULTS_PER_PAGE);

        try {
            AdzunaSearchResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/v1/api/jobs/{country}/search/1")
                                .queryParam("app_id", appId)
                                .queryParam("app_key", appKey)
                                .queryParam("results_per_page", pageSize);
                        if (what != null && !what.isBlank()) {
                            uriBuilder.queryParam("what", what);
                        }
                        if (where != null && !where.isBlank()) {
                            uriBuilder.queryParam("where", where);
                        }
                        return uriBuilder.build(country);
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    })
                    .body(AdzunaSearchResponse.class);

            if (response == null || response.results() == null) {
                return Collections.emptyList();
            }

            return response.results().stream()
                    .map(AdzunaJobResult::toAdzunaJob)
                    .toList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    public static class AdzunaJob {

        private final String title;
        private final String companyName;
        private final String location;
        private final String description;
        private final String redirectUrl;

        public AdzunaJob(
                String title,
                String companyName,
                String location,
                String description,
                String redirectUrl) {
            this.title = title;
            this.companyName = companyName;
            this.location = location;
            this.description = description;
            this.redirectUrl = redirectUrl;
        }

        public String getTitle() {
            return title;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getLocation() {
            return location;
        }

        public String getDescription() {
            return description;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaSearchResponse(List<AdzunaJobResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaJobResult(
            String title,
            String description,
            @JsonProperty("redirect_url") String redirectUrl,
            AdzunaCompany company,
            AdzunaLocation location) {

        private AdzunaJob toAdzunaJob() {
            return new AdzunaJob(
                    title,
                    company != null ? company.displayName() : null,
                    location != null ? location.displayName() : null,
                    description,
                    redirectUrl);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaCompany(@JsonProperty("display_name") String displayName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AdzunaLocation(@JsonProperty("display_name") String displayName) {
    }
}
