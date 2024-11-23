package com.sean.backend;

import java.io.IOException;
import java.net.URI;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final SpotifyApi spotifyApi;

    public AuthController(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    @GetMapping("/login")
    public RedirectView spotifyLogin() {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope("playlist-modify-public, user-read-private, user-read-email, user-top-read")
            .show_dialog(true)
            .build();
        final URI uri = authorizationCodeUriRequest.execute();
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(uri.toString());
        return redirectView;
    }

    @GetMapping("/get-user-code")
    public ResponseEntity<String> getSpotifyUserCode(@RequestParam("code") String userCode, HttpServletResponse response) {
        if (userCode == null || userCode.isEmpty()) {
            return ResponseEntity.badRequest().body("No authorization code provided.");
        }

        try {
            // Exchange the authorization code for an access token
            AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(userCode).build();
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            // Set the tokens in the SpotifyApi instance
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

            // Optional logging (remove or modify as needed)
            System.out.println("Access Token: " + authorizationCodeCredentials.getAccessToken());
            System.out.println("Refresh Token: " + authorizationCodeCredentials.getRefreshToken());
            System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());

            // Redirect the user back to the frontend after successful exchange
            response.sendRedirect("http://localhost:3000/generate");
            return ResponseEntity.ok("Token exchange successful.");

        } catch (IOException | SpotifyWebApiException | org.apache.hc.core5.http.ParseException e) {
            // Log the error for troubleshooting
            System.err.println("Error during token exchange: " + e.getMessage());
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error during token exchange.");
            } catch (IOException ioException) {
                System.err.println("Error sending error response: " + ioException.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during token exchange.");
        }
    }
}