package co.evreaka.fusedlocation.locationfused;

import android.location.Location;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;



import java.lang.reflect.Type;

public class EvrekaLocation extends Location
{
    public EvrekaLocation(Location l)
    {
        super(l);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.
            append("Lat: ").
            append(getLatitude()).
            append("\n").

            append(" Long: ").
            append(getLongitude()).
            append("\n").

            append(" Timestamp: ").
            append(getTime()).
            append("\n").

            append(" Accuracy: ").
            append(getAccuracy()).
            append("\n").

            append(" Bearing: ").
            append(getBearing()).
            append("\n").

            append(" Provider: ").
            append(getProvider()).
            append("\n").

            append(" Speed: ").
            append(getSpeed()).
            append("\n");


        return sb.toString();
    }
}
