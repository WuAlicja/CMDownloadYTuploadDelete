import com.google.api.client.http.InputStreamContent;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ClickMeetingDownload {

    private String apiKey;
    private String myPath;
    private boolean isActive;
    private WhichVideoService videoService;

    public ClickMeetingDownload(String apiKey, String myPath,boolean isActive) {
        this.apiKey = apiKey;
        this.myPath = myPath;
        this.isActive=isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }


    public void processConferenceRecordings(WhichVideoService videoService) throws Exception {
        String myUrlActive;
        if(isActive) {
            myUrlActive = "https://api.clickmeeting.com/v1/conferences/active?api_key=" + apiKey;
        }else {
            myUrlActive = "https://api.clickmeeting.com/v1/conferences/inactive?api_key=" + apiKey;
        }
    try {
        JSONArray arrayActive = getArrayOfJSONObjects(myUrlActive);
        int arrayActiveSize = arrayActive.length();

        for (int i = 0; i < arrayActiveSize; i++) {

            int roomId = getRoomId(arrayActive, i);
            String urlRecordings = "https://api.clickmeeting.com/v1/conferences/"
                    + roomId + "/recordings?api_key=" + apiKey;
            JSONArray arrayRecordings = getArrayOfJSONObjects(urlRecordings);

            int recorderListSize = getRecorderListSize(arrayActive, i);

            List<String> urlRecordingActiveList = getRecordingsUrlList(recorderListSize, arrayActive, i);
            if (urlRecordingActiveList == null)
                continue;

            for (int k = 0; k < 2; k++) { //zmienic recorderListSize na konkretna liczbe wieksza od 0 jesli nie chcemy ladowac wszystkich video
                String fileName = "recordingActive" + +i + k + ".mp4";
                saveRecording(myPath, urlRecordingActiveList, fileName, k + 1);
                if (videoService.equals(WhichVideoService.YOUTUBE)) {
                    try {
                        long recordingFileSize = getRecordingFileSize(arrayRecordings, k);
                        if (recordingFileSize <= 137438953472L) {

                            Upload upload = new YoutubeUpload();
                            String response = upload.uploadVideo(myPath, fileName, "Video ze szkolenia " + i + k,
                                    "Video z ClickMeeting numer " + i + k, "public");

                            JSONObject responseObject = new JSONObject(response);

                            int recordingId = getRecordingId(arrayRecordings, k);
                            deleteRecording(apiKey, roomId, recordingId);


                        } else {
                            System.out.println("Rozmiar tego video jest za duzy, zeby moglo zostac zaladowane na" +
                                    " Youtube za pomoca tej aplikacji");
                        }
                    } catch (JSONException e) {
                        System.out.println("Nie udalo sie zaladowac video na Youtube");
                    }

                }
            }
        }
    }catch (
    MalformedURLException e) {
        System.out.println("URL nie jest prawidlowe");
    } catch (
    FileNotFoundException e) {
        System.out.println("Nie znaleziono pliku");
    } catch (
    IOException e) {
        System.out.println("Nieprawidlowy API key lub Limit quotes dla Twojego konta na YouTube został wyczerpany");
    }
}

    private static void deleteRecording(String apiKey, int roomId, int recordingId) throws IOException {
        String deleteRecording = "https://api.clickmeeting.com/v1/conferences/" + roomId +
                "/recordings/" + recordingId + "?api_key=" + apiKey;
        System.out.println(deleteRecording);
        URL urlDelete = new URL(deleteRecording);
        HttpURLConnection con = (HttpURLConnection) urlDelete.openConnection();
        con.setRequestMethod("DELETE");

        int responseCode = con.getResponseCode();

        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == 200) {
            System.out.println("Udalo sie skasowac video z ClickMeeting.");
        } else {
            System.out.println("Nie udalo sie skasowac video z ClickMeeting");
        }
    }

    private static int getRecordingId(JSONArray arrayRecordings, int recordingOrderNo) {
        return arrayRecordings.getJSONObject(recordingOrderNo).getInt("id");
    }

    private static int getRoomId(JSONArray arrayOfActiveConferences, int conferenceOrderNo) {
        return arrayOfActiveConferences.getJSONObject(conferenceOrderNo).getInt("id");
    }

    private static int getRecorderListSize(JSONArray arrayOfActiveConferences, int conferenceOrderNo) {
        return arrayOfActiveConferences.getJSONObject(conferenceOrderNo).getJSONArray("recorder_list").length();
    }

    private static long getRecordingFileSize(JSONArray arrayRecordings, int recordingOrderNo) {
        return arrayRecordings.getJSONObject(recordingOrderNo).getInt("recording_file_size");
    }

    private static JSONArray getArrayOfJSONObjects(String url) throws IOException {
        URL currentUrl = new URL(url);

        JSONTokener tokenerActive = new JSONTokener(currentUrl.openStream());
        return new JSONArray(tokenerActive);
    }


    public static void saveRecording(String myPath, List<String> urlRecordingsList,
                                     String fileName, int recordingOrderNo) throws IOException {
        String path = myPath + fileName;
        InputStream inputStream = new URL(urlRecordingsList.get(recordingOrderNo - 1)).openStream();
        Files.copy(inputStream, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
    }

    public static List<String> getRecordingsUrlList(int recorderListSize, JSONArray arrayOfActiveConferences, int conferenceOrderNo) {
        List<String> urlRecordingActiveList = new ArrayList<>();

        if (recorderListSize > 0) {
            for (int j = 0; j < recorderListSize; j++) {

                urlRecordingActiveList.add(arrayOfActiveConferences.getJSONObject(conferenceOrderNo).getJSONArray("recorder_list").getString(j));
            }
            return urlRecordingActiveList;
        }
        System.out.println("Ta konferencja nie ma zadnych nagran");
        return null;
    }
}
