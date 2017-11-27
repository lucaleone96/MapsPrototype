package lucaleone.mapsprototype;

import android.location.Location;
import android.os.Environment;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GPXWriter {

    public static void writePathLocation(File file, String n, List<List<Location>> listOfTrack) throws IOException {
        if(checkExternalMedia()) {
            String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapsPrototype\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n<trk>\n";
            String name = "<name>" + n + "</name>\n";


            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            StringBuilder sb = new StringBuilder();
            for (List<Location> singleTrack : listOfTrack) {
                sb.append("<trkseg>\n");
                for(Location l : singleTrack) {
                    sb.append("<trkpt lat=\"" + l.getLatitude() + "\" lon=\"" + l.getLongitude() + "\"><time>" + df.format(new Date(l.getTime())) + "</time></trkpt>\n");
                }
                sb.append("</trkseg>\n");
            }

            String footer = "</trk>\n</gpx>";
            FileWriter writer = new FileWriter(file, false);
            writer.append(header);
            writer.append(name);
            writer.append(sb);
            writer.append(footer);
            writer.flush();
            writer.close();
        } else throw new FileNotFoundException("External storage not exist");

    }

    public static void writePathLatLng(File file, String n, List<List<LatLng>> listOfTrack) throws IOException {
        if(checkExternalMedia()) {
            String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapsPrototype\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n<trk>\n";
            String name = "<name>" + n + "</name>\n";

            StringBuilder sb = new StringBuilder();
            for(List<LatLng> singleTrack : listOfTrack) {
                sb.append("<trkseg>\n");
                for(LatLng l : singleTrack) {
                    sb.append("<trkpt lat=\"" + l.latitude + "\" lon=\"" + l.longitude + "\"></trkpt>\n");
                }
                sb.append("</trkseg>\n");
            }

            String footer = "</trk>\n</gpx>";
            FileWriter writer = new FileWriter(file, false);
            writer.append(header);
            writer.append(name);
            writer.append(sb);
            writer.append(footer);
            writer.flush();
            writer.close();
        } else throw new FileNotFoundException("External storage not exist");

    }

    private static boolean checkExternalMedia(){
        String state = Environment.getExternalStorageState();

        // The external media is available
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
