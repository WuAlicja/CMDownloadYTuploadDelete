import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class YoutubeUpload implements Upload {

    public boolean uploadVideo(String path, String fileName, String title, String description, String privacyStatus, long recordingFileSize) throws Exception {

        try {
            if (recordingFileSize <= 137438953472L) {
                YouTube youtubeService = YoutubeMethods.getService();

                // Define the Video object, which will be uploaded as the request body.
                Video video = new Video();

                // Add the snippet object property to the Video object.
                VideoSnippet snippet = new VideoSnippet();
                snippet.setCategoryId("22");
                snippet.setDescription(description);
                snippet.setTitle(title);
                video.setSnippet(snippet);

                // Add the status object property to the Video object.
                VideoStatus status = new VideoStatus();
                status.setPrivacyStatus(privacyStatus);
                video.setStatus(status);

                File mediaFile = new File(path + fileName);
                InputStreamContent mediaContent =
                        new InputStreamContent("application/octet-stream",
                                new BufferedInputStream(new FileInputStream(mediaFile)));
                mediaContent.setLength(mediaFile.length());

                // Define and execute the API request
                YouTube.Videos.Insert request = youtubeService.videos()
                        .insert("snippet,status", video, mediaContent);
                Video response = request.execute();

                System.out.println(response);
                JSONObject responseObject = new JSONObject(response);
                return true;
            } else {
                System.out.println("Rozmiar tego video jest za duzy, zeby moglo zostac zaladowane na" +
                        " Youtube za pomoca tej aplikacji");
                return false;
            }
        } catch (JSONException e) {
            System.out.println("Nie udalo sie zaladowac video na Youtube");
            return false;
        }
    }
}


