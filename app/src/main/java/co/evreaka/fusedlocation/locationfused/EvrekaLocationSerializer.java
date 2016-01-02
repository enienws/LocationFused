package co.evreaka.fusedlocation.locationfused;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class EvrekaLocationSerializer implements JsonSerializer<EvrekaLocation>
{

    @Override
    public JsonElement serialize(EvrekaLocation evrekaLocation, Type type, JsonSerializationContext jsonSerializationContext)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Latitude", evrekaLocation.getLatitude());
        jsonObject.addProperty("Longitude", evrekaLocation.getLongitude());
        jsonObject.addProperty("Timestamp", evrekaLocation.getTime());
        jsonObject.addProperty("Accuracy", evrekaLocation.getAccuracy());
        jsonObject.addProperty("Bearing", evrekaLocation.getBearing());
        jsonObject.addProperty("Provider", evrekaLocation.getProvider());
        jsonObject.addProperty("Speed", evrekaLocation.getSpeed());
        return jsonObject;
    }
}
