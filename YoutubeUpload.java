
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;

public class YoutubeUpload {
    private static final String CLIENT_SECRETS = "client_secret.json";
    private static final Collection<String> SCOPES =
            Arrays.asList("https://www.googleapis.com/auth/youtube.upload");

    private static final String APPLICATION_NAME = "Youtube Video Upload";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static Credential authorize(final NetHttpTransport httpTransport) throws Exception {
        // Load client secrets.
        InputStream in = YoutubeUpload.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                        .build();
        Credential credential =
                new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    public static YouTube getService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args)
            throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Podaj API key do Click Meeting");
        String apiKey = scanner.next();
        String myUrlActive = "https://api.clickmeeting.com/v1/conferences/active?api_key=" + apiKey;
        System.out.println("Podaj sciezke do przechowywania danych na dysku w formacie typu /Users/alicja/Downloads/ ," +
                " pamietaj o ostatnim slashu");
        String myPath = scanner.nextLine();

        try {

            URL urlActive = new URL(myUrlActive);

            JSONTokener tokenerActive = new JSONTokener(urlActive.openStream());
            JSONArray arrayActive = new JSONArray(tokenerActive);
            int arrayActiveSize = arrayActive.length();

            List<String> urlRecordingActiveList = new ArrayList<>();
            for (int i = 0; i < arrayActiveSize; i++) {

                int roomId = arrayActive.getJSONObject(i).getInt("id");
                URL urlRecordings = new URL("https://api.clickmeeting.com/v1/conferences/"
                        + roomId + "/recordings?api_key=" + apiKey);
                JSONTokener tokenerRecordings = new JSONTokener(urlRecordings.openStream());
                JSONArray arrayRecordings = new JSONArray(tokenerRecordings);


                int recorderListSize = arrayActive.getJSONObject(i).getJSONArray("recorder_list").length();
                if (recorderListSize > 0) {
                    for (int j = 0; j < recorderListSize; j++) {

                        urlRecordingActiveList.add(arrayActive.getJSONObject(i).getJSONArray("recorder_list").getString(j));
                    }
                    for (int k = 0; k < recorderListSize; k++) { //zmienic recorderListSize na konkretna liczbe wieksza od 0 jesli nie chcemy ladowac wszystkich video

                        String fileName = "recordingActive" + +i + k + ".mp4";
                        String path = myPath + fileName;
                        InputStream inputStream = new URL(urlRecordingActiveList.get(k)).openStream();
                        Files.copy(inputStream, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
                        long recordingFileSize = arrayRecordings.getJSONObject(k).getInt("recording_file_size");
                        if (recordingFileSize <= 137438953472L) {
                            YouTube youtubeService = getService();

                            // Define the Video object, which will be uploaded as the request body.
                            Video video = new Video();

                            // Add the snippet object property to the Video object.
                            VideoSnippet snippet = new VideoSnippet();
                            snippet.setCategoryId("22");
                            snippet.setDescription("Video z ClickMeeting numer " + i + k);
                            snippet.setTitle("Video ze szkolenia " + i + k);
                            video.setSnippet(snippet);

                            // Add the status object property to the Video object.
                            VideoStatus status = new VideoStatus();
                            status.setPrivacyStatus("public");
                            video.setStatus(status);

                            File mediaFile = new File(myPath + fileName);
                            InputStreamContent mediaContent =
                                    new InputStreamContent("application/octet-stream",
                                            new BufferedInputStream(new FileInputStream(mediaFile)));
                            mediaContent.setLength(mediaFile.length());

                            // Define and execute the API request
                            YouTube.Videos.Insert request = youtubeService.videos()
                                    .insert("snippet,status", video, mediaContent);
                            Video response = request.execute();
                            System.out.println(response);
                            try {
                                JSONObject responseObject = new JSONObject(response);

                                int recordingId = arrayRecordings.getJSONObject(k).getInt("id");
                                String deleteRecording = "https://api.clickmeeting.com/v1/conferences/" + roomId +
                                        "/recordings/" + recordingId + "?" + apiKey;
                                URL urlDelete = new URL(deleteRecording);
                                HttpURLConnection con = (HttpURLConnection) urlDelete.openConnection();
                                con.setRequestMethod("DELETE");

                                int responseCode = con.getResponseCode();

                                System.out.println("GET Response Code :: " + responseCode);
                                if (responseCode == 200) {
                                    System.out.println("Udalo sie skasowac video.");
                                }
                            } catch (JSONException e) {
                                System.out.println("Nie udalo sie zaladowac video na Youtube");
                            }
                        } else {
                            System.out.println("Rozmiar tego video jest za duzy, zeby moglo zostac zaladowane na" +
                                    " Youtube za pomoca tej aplikacji");
                        }
                    }
                } else {
                    System.out.println("Nie ma zadnych nagran");
                }
            }
        } catch (
                MalformedURLException e) {
            System.out.println("URL nie jest prawidlowe");
        } catch (
                FileNotFoundException e) {
            System.out.println("Nie znaleziono pliku");
        } catch (
                IOException e) {
            System.out.println("Limit quotes dla Twojego konta na YouTube został wyczerpany");
        }
    }
}