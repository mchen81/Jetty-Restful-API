package rawHttpServer.handlers;

import com.google.gson.stream.JsonWriter;
import hotelapp.HotelDataDriver;
import hotelapp.ThreadSafeHotelData;
import hotelapp.bean.Hotel;
import rawHttpServer.HttpHandler;
import rawHttpServer.HttpRequest;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handler for /hotelInfo
 */
public class HotelHandler implements HttpHandler {

    @Override
    public void processRequest(HttpRequest request, PrintWriter writer) {
        ThreadSafeHotelData hotelData = HotelDataDriver.hotelData;


        String hotelId = request.getValue("hotelId");
        hotelId = hotelId == null ? "-1" : hotelId;
        Hotel hotel = hotelData.getHotelInstance(hotelId);

        JsonWriter jsonWriter = new JsonWriter(writer);
        try {
            jsonWriter.beginObject();
            if (hotel == null) {
                jsonWriter.name("success").value(false);
                jsonWriter.name("hotelId").value("invalid");
            } else {
                jsonWriter.name("success").value(true);
                jsonWriter.name("hotelId").value(hotel.getHotelId());
                jsonWriter.name("name").value(hotel.getName());
                jsonWriter.name("addr").value(hotel.getStreetAddress());
                jsonWriter.name("city").value(hotel.getCity());
                jsonWriter.name("state").value(hotel.getState());
                jsonWriter.name("lat").value(hotel.getLatitude().toString());
                jsonWriter.name("lng").value(hotel.getLongitude().toString());
            }
            jsonWriter.endObject();
            writer.println();

        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot process Hotel Info");
        }
    }

}
