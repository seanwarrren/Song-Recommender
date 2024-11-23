package com.sean.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.AudioFeatures;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Recommendations;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.browse.GetRecommendationsRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetAudioFeaturesForSeveralTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetAudioFeaturesForTrackRequest;

@Service

public class SongSimilarityService {

    private final SpotifyApi spotifyApi;

    public SongSimilarityService(SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }
    
    // search for the track
    public String getTrackId(String song, String artist) throws IOException, SpotifyWebApiException, ParseException {
        SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks(song + " " + artist).build();
        Paging<Track> trackPaging = searchTracksRequest.execute();
        return trackPaging.getItems()[0].getId();
    }

    // retrive audio features for the track
    public AudioFeatures getAudioFeatures(String trackId) throws IOException, SpotifyWebApiException, ParseException {
        GetAudioFeaturesForTrackRequest audioFeaturesRequest = spotifyApi.getAudioFeaturesForTrack(trackId).build();
        return audioFeaturesRequest.execute();
    }

    // generate recommendations using spotify recommendations service
    public List<Track> getRecommendations(String seedTrackId) throws IOException, SpotifyWebApiException, ParseException {
        GetRecommendationsRequest recommendationsRequest = spotifyApi.getRecommendations()
            .limit(100)
            .seed_tracks(seedTrackId)
            .build();
        Recommendations recommendations = recommendationsRequest.execute();
        return Arrays.asList(recommendations.getTracks());
    }

    // extract audio features for recommended songs
    public List<AudioFeatures> getAudioFeaturesForTracks(List<String> trackIds) throws IOException, SpotifyWebApiException, ParseException {
        GetAudioFeaturesForSeveralTracksRequest featuresRequest = spotifyApi.getAudioFeaturesForSeveralTracks(trackIds.toArray(new String[0])).build();
        return Arrays.asList(featuresRequest.execute());
    }

    // represent each song's audio features as a vector
    // calculate cosine similarity between the vectors
    public double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // calculate knn to refine the recommendation
    public List<Track> refineRecommendations(List<Pair<Track, AudioFeatures>> trackFeaturePairs, AudioFeatures inputSongFeatures) {
        List<Pair<Track, Double>> trackSimilarityList = new ArrayList<>();

        // calculate similarity between input song and recommended songs
        for (Pair<Track, AudioFeatures> trackFeaturePair : trackFeaturePairs) {
        AudioFeatures feature = trackFeaturePair.getValue();

            double[] inputVector = {
                inputSongFeatures.getAcousticness(),
                inputSongFeatures.getDanceability(),
                inputSongFeatures.getEnergy(),
                inputSongFeatures.getInstrumentalness(),
                inputSongFeatures.getKey(),
                inputSongFeatures.getLiveness(),
                inputSongFeatures.getLoudness(),
                inputSongFeatures.getSpeechiness(),
                inputSongFeatures.getTempo(),
                inputSongFeatures.getValence(),
            };
            double[] trackVector = {
                feature.getAcousticness(),
                feature.getDanceability(),
                feature.getEnergy(),
                feature.getInstrumentalness(),
                feature.getKey(),
                feature.getLiveness(),
                feature.getLoudness(),
                feature.getSpeechiness(),
                feature.getTempo(),
                feature.getValence(),
            };

            // calculate cosine similarity
            double similarity = calculateCosineSimilarity(inputVector, trackVector);
            trackSimilarityList.add(new Pair<>(trackFeaturePair.getKey(), similarity));
        }

        // sort tracks by similarity in descending order
        trackSimilarityList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // select top 25 songs
        List<Track> top25 = new ArrayList<>();
        for (int i = 0; i < Math.min(25, trackSimilarityList.size()); i++) {
            top25.add(trackSimilarityList.get(i).getKey());
        }

        return top25;
    }

}

class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
