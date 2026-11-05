package dev.strongholdfinder.core;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/** Saves each ray as what it was built from. An older save has only the angle form, so it reads unchanged. */
final class ThrowJsonAdapter implements JsonSerializer<Throw>, JsonDeserializer<Throw> {
    @Override
    public JsonElement serialize(Throw t, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("id", t.id());
        json.addProperty("x", t.x());
        json.addProperty("z", t.z());
        switch (t.ray()) {
            case AngleRay angle -> json.addProperty("yaw", angle.yaw());
            case TwoPointRay two -> {
                json.addProperty("toX", two.through().x());
                json.addProperty("toZ", two.through().z());
            }
        }
        json.addProperty("on", t.on());
        return json;
    }

    @Override
    public Throw deserialize(JsonElement element, Type type, JsonDeserializationContext context) {
        JsonObject json = element.getAsJsonObject();
        Point origin = new Point(json.get("x").getAsDouble(), json.get("z").getAsDouble());
        Ray ray = json.has("toX")
                ? new TwoPointRay(origin, new Point(json.get("toX").getAsDouble(), json.get("toZ").getAsDouble()))
                : new AngleRay(origin, json.get("yaw").getAsDouble());
        return new Throw(json.get("id").getAsInt(), ray, json.get("on").getAsBoolean());
    }
}
