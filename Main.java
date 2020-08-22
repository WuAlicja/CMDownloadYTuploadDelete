import java.util.Scanner;

public class Main {

    public static void main(String[] args)
            throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Podaj API key do Click Meeting");
        String apiKey = scanner.next();

        System.out.println("Podaj sciezke do przechowywania danych na dysku w formacie typu /Users/alicja/Downloads/ ," +
                " pamietaj o ostatnim slashu");
        String myPath = scanner.nextLine();

        ClickMeetingDownload clickMeetingDownloadInactive = new ClickMeetingDownload(apiKey, myPath,false);
        clickMeetingDownloadInactive.processConferenceRecordings();
    }
}