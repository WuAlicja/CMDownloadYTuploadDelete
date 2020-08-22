
import com.clickntap.vimeo.Vimeo;
import com.clickntap.vimeo.VimeoException;
import com.clickntap.vimeo.VimeoResponse;

import java.io.File;
import java.io.IOException;

public class VimeoUpload implements Upload{
    String vimeoToken;

    public VimeoUpload(String vimeoToken) {
        this.vimeoToken = vimeoToken;
    }

    public  String uploadVideo(String path, String fileName, String title, String description, String privacyStatus) throws IOException, VimeoException {
        Vimeo vimeo = new Vimeo(this.vimeoToken);

        //add a video
        boolean upgradeTo1080 = true;
        String videoEndPoint = vimeo.addVideo(new File(path+fileName), upgradeTo1080);

        //get video info
        VimeoResponse info = vimeo.getVideoInfo(videoEndPoint);
        System.out.println(info);

        //edit video
        String license = ""; //see Vimeo API Documentation
        String privacyView = "disable"; //see Vimeo API Documentation
        boolean reviewLink = false;
        vimeo.updateVideoMetadata(videoEndPoint, title, description, license, privacyView, privacyStatus, reviewLink);

        //add video privacy domain
        //vimeo.addVideoPrivacyDomain(videoEndPoint, "clickntap.com");

        //delete video
        //vimeo.removeVideo(videoEndPoint);
        return videoEndPoint;
    }

}

