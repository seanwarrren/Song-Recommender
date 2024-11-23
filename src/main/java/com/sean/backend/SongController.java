package com.sean.backend;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.michaelthelin.spotify.model_objects.specification.AudioFeatures;
import se.michaelthelin.spotify.model_objects.specification.Track;

@RestController
@RequestMapping("/api/songs")

public class SongController {
    
    private final SongSimilarityService songSimilarityService;

    public SongController(SongSimilarityService songSimilarityService) {
        this.songSimilarityService = songSimilarityService;
    }

    @PostMapping("/generate-similar")
    public ResponseEntity<List<String>> generateSimilarSongs(@RequestBody SongRequest request) {
        try {
            String trackId = songSimilarityService.getTrackId(request.getSong(), request.getArtist());
            AudioFeatures inputFeatures = songSimilarityService.getAudioFeatures(trackId);
            List<Track> recommendedTracks = songSimilarityService.getRecommendations(trackId);
            List<String> trackIds = recommendedTracks.stream().map(Track::getId).toList();
            List<AudioFeatures> recommendedFeatures = songSimilarityService.getAudioFeaturesForTracks(trackIds);

            List<Pair<Track, AudioFeatures>> trackFeaturePairs = new ArrayList<>();
            for (int i = 0; i < recommendedTracks.size(); i++) {
                trackFeaturePairs.add(new Pair<>(recommendedTracks.get(i), recommendedFeatures.get(i)));
            }

            List<Track> refinedRecommendations = songSimilarityService.refineRecommendations(trackFeaturePairs, inputFeatures);

            List<String> songNames = refinedRecommendations.stream()
                .map(track -> track.getName() + "by" + track.getArtists()[0].getName())
                .toList();

            return ResponseEntity.ok(songNames);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of("Error: " + e.getMessage()));
        }
    }
}

class SongRequest {
    private String song;
    private String artist;

    public void setString(String song) {
        this.song = song;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSong() {
        return song;
    }

    public String getArtist() {
        return artist;
    }
}