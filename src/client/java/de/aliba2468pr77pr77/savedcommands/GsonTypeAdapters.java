package de.aliba2468pr77pr77.savedcommands;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.InputConstants;

import java.io.IOException;

public class GsonTypeAdapters {
    public static class InputConstantsKeyAdapter extends TypeAdapter<InputConstants.Key> {
        @Override
        public void write(JsonWriter out, InputConstants.Key key) throws IOException {
            if (key == null) {
                out.nullValue();
            } else {
                out.value(key.getName()); // z.B. "key.keyboard.w"
            }
        }

        @Override
        public InputConstants.Key read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return InputConstants.getKey(in.nextString());
        }
    }

    public static class KeybindCombinationMigrater implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != SavedCommandManager.CommandData.KeybindCombination.class) {
                return null;
            }

            // Normal TypeAdapter:
            TypeAdapter<SavedCommandManager.CommandData.KeybindCombination> delegate = gson.getDelegateAdapter(
                    this, TypeToken.get(SavedCommandManager.CommandData.KeybindCombination.class));

            TypeAdapter<SavedCommandManager.CommandData.KeybindCombination> migratingAdapter = new TypeAdapter<>() {

                @Override
                public void write(JsonWriter out, SavedCommandManager.CommandData.KeybindCombination value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public SavedCommandManager.CommandData.KeybindCombination read(JsonReader in) {
                    JsonObject obj = Streams.parse(in).getAsJsonObject();

                    if (obj.has("keybindType") && obj.has("keybindCode")) {
                        // Migrating old type:
                        SavedCommandManager.CommandData.KeybindCombination result =
                                new SavedCommandManager.CommandData.KeybindCombination(true);
                        JsonArray types = obj.getAsJsonArray("keybindType");
                        JsonArray codes = obj.getAsJsonArray("keybindCode");

                        for (int i = 0; i < codes.size(); i++) {
                            result.keys.add(InputConstants.Type.valueOf(types.get(i).getAsString()).getOrCreate(codes.get(i).getAsInt()));
                        }

                        return result;
                    }

                    // New type is parsed like usual
                    return delegate.fromJsonTree(obj);
                }
            };

            return (TypeAdapter<T>) migratingAdapter;
        }
    }
}