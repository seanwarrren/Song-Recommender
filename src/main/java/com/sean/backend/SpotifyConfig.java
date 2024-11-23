package com.sean.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.net.URI;

@Configuration
public class SpotifyConfig {

    private static final URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:3000/generate");
    private static final String CLIENT_ID = "13e244917ca64efb96556f753fcd032b";
    private static final String CLIENT_SECRET = "ded93049447347769bcea9b2327c7ba6";

    @Bean
    public SpotifyApi spotifyApi() {
        return new SpotifyApi.Builder()
            .setClientId(CLIENT_ID)
            .setClientSecret(CLIENT_SECRET)
            .setRedirectUri(redirectUri)
            .build();
    }
}
