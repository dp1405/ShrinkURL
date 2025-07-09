package org.stir.shrinkurl.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.stir.shrinkurl.dto.GoogleTokenResponse;
import org.stir.shrinkurl.dto.GoogleUserInfo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OAuth2GoogleService {
    @Value("${oauth2.google.client-id}")
    private String clientId;

    @Value("${oauth2.google.client-secret}")
    private String clientSecret;

    @Value("${oauth2.google.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.google.scope}")
    private List<String> scopes;

    private final RestTemplate restTemplate;

    public OAuth2GoogleService(){
        this.restTemplate = new RestTemplate();

        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                log.error("OAuth2 Service Error: {} - {}", response.getStatusCode(), response.getStatusText());
                super.handleError(response);
            }
        });
    }

    public String buildAuthorizationUrl(String state){
        String scopeString = String.join(" ", scopes);

        return UriComponentsBuilder
            .fromUriString("https://accounts.google.com/o/oauth2/auth")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", scopeString)
            .queryParam("state", state)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .build()
            .toString();
    }

    public GoogleTokenResponse exchangeCodeForToken(String authCode){
        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", authCode);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String,String>> request = new HttpEntity<>(params,headers);

        try{
            ResponseEntity<GoogleTokenResponse> response=restTemplate.postForEntity(tokenEndpoint, request, GoogleTokenResponse.class);

            log.info("OAuth2 Token Exchange Successful!");

            return response.getBody();
        }
        catch(Exception e){
            log.error("Failed OAuth2 Token Exchange!", e);
            throw new OAuth2AuthenticationException("Failed to exchange code for token: " + e.getMessage());
        }
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try{
            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, request, GoogleUserInfo.class);

            return response.getBody();
        }
        catch(Exception e){
            log.error("OAuth2 User Info Retreival Failed!");
            throw new OAuth2AuthenticationException("Failed to get user info: " + e.getMessage());
        }
    }
}
