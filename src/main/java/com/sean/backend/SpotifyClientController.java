package com.sean.backend;

import org.springframework.http.HttpHeaders;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class SpotifyClientController {

    private static final String CLIENT_ID = "13e244917ca64efb96556f753fcd032b";
    private static final String CLIENT_SECRET = "ded93049447347769bcea9b2327c7ba6";

    @GetMapping("/generate-token")
    public ResponseEntity<String> generateToken() {
        try {
            String auth = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            RestTemplate restTemplate = new RestTemplate();

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");

            String body = "grant_type=client_credentials";
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token", 
                new org.springframework.http.HttpEntity<>(body, headers), 
                Map.class
            );

            String accessToken = (String) response.getBody().get("access_token");
            return ResponseEntity.ok(accessToken);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating token: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<String> searchTracks(@RequestParam String query) {
        try {
            String accessToken = generateToken().getBody(); // Get the token dynamically

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            String url = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=5";

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, headers);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching tracks: " + e.getMessage());
        }
    }
}