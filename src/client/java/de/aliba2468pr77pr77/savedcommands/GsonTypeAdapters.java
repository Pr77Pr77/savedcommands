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
                            result.keys.add(InputConstants.getKey(getTranslationKeyFromGLFWcode(types.get(i).getAsString(), codes.get(i).getAsInt())));
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

    public static String getTranslationKeyFromGLFWcode(String type, int code) { // Done for compatibility with configs of old versions of the mod.
        if ("MOUSE".equalsIgnoreCase(type)) {
            return switch (code) {
                case 0 -> "key.mouse.left";
                case 1 -> "key.mouse.right";
                case 2 -> "key.mouse.middle";
                default -> "key.mouse." + code;
            };
        }

        return switch (code) {
            case 32 -> "key.keyboard.space";
            case 39 -> "key.keyboard.apostrophe";
            case 44 -> "key.keyboard.comma";
            case 45 -> "key.keyboard.minus";
            case 46 -> "key.keyboard.period";
            case 47 -> "key.keyboard.slash";
            case 59 -> "key.keyboard.semicolon";
            case 61 -> "key.keyboard.equal";
            case 65 -> "key.keyboard.a";
            case 66 -> "key.keyboard.b";
            case 67 -> "key.keyboard.c";
            case 68 -> "key.keyboard.d";
            case 69 -> "key.keyboard.e";
            case 70 -> "key.keyboard.f";
            case 71 -> "key.keyboard.g";
            case 72 -> "key.keyboard.h";
            case 73 -> "key.keyboard.i";
            case 74 -> "key.keyboard.j";
            case 75 -> "key.keyboard.k";
            case 76 -> "key.keyboard.l";
            case 77 -> "key.keyboard.m";
            case 78 -> "key.keyboard.n";
            case 79 -> "key.keyboard.o";
            case 80 -> "key.keyboard.p";
            case 81 -> "key.keyboard.q";
            case 82 -> "key.keyboard.r";
            case 83 -> "key.keyboard.s";
            case 84 -> "key.keyboard.t";
            case 85 -> "key.keyboard.u";
            case 86 -> "key.keyboard.v";
            case 87 -> "key.keyboard.w";
            case 88 -> "key.keyboard.x";
            case 89 -> "key.keyboard.y";
            case 90 -> "key.keyboard.z";
            case 91 -> "key.keyboard.left.bracket";
            case 92 -> "key.keyboard.backslash";
            case 93 -> "key.keyboard.right.bracket";
            case 96 -> "key.keyboard.grave.accent";
            case 161 -> "key.keyboard.world.1";
            case 162 -> "key.keyboard.world.2";
            case 256 -> "key.keyboard.escape";
            case 257 -> "key.keyboard.enter";
            case 258 -> "key.keyboard.tab";
            case 259 -> "key.keyboard.backspace";
            case 260 -> "key.keyboard.insert";
            case 261 -> "key.keyboard.delete";
            case 262 -> "key.keyboard.right";
            case 263 -> "key.keyboard.left";
            case 264 -> "key.keyboard.down";
            case 265 -> "key.keyboard.up";
            case 266 -> "key.keyboard.page.up";
            case 267 -> "key.keyboard.page.down";
            case 268 -> "key.keyboard.home";
            case 269 -> "key.keyboard.end";
            case 280 -> "key.keyboard.caps.lock";
            case 281 -> "key.keyboard.scroll.lock";
            case 282 -> "key.keyboard.num.lock";
            case 283 -> "key.keyboard.print.screen";
            case 284 -> "key.keyboard.pause";
            case 290 -> "key.keyboard.f1";
            case 291 -> "key.keyboard.f2";
            case 292 -> "key.keyboard.f3";
            case 293 -> "key.keyboard.f4";
            case 294 -> "key.keyboard.f5";
            case 295 -> "key.keyboard.f6";
            case 296 -> "key.keyboard.f7";
            case 297 -> "key.keyboard.f8";
            case 298 -> "key.keyboard.f9";
            case 299 -> "key.keyboard.f10";
            case 300 -> "key.keyboard.f11";
            case 301 -> "key.keyboard.f12";
            case 302 -> "key.keyboard.f13";
            case 303 -> "key.keyboard.f14";
            case 304 -> "key.keyboard.f15";
            case 305 -> "key.keyboard.f16";
            case 306 -> "key.keyboard.f17";
            case 307 -> "key.keyboard.f18";
            case 308 -> "key.keyboard.f19";
            case 309 -> "key.keyboard.f20";
            case 310 -> "key.keyboard.f21";
            case 311 -> "key.keyboard.f22";
            case 312 -> "key.keyboard.f23";
            case 313 -> "key.keyboard.f24";
            case 314 -> "key.keyboard.f25";
            case 320 -> "key.keyboard.keypad.0";
            case 321 -> "key.keyboard.keypad.1";
            case 322 -> "key.keyboard.keypad.2";
            case 323 -> "key.keyboard.keypad.3";
            case 324 -> "key.keyboard.keypad.4";
            case 325 -> "key.keyboard.keypad.5";
            case 326 -> "key.keyboard.keypad.6";
            case 327 -> "key.keyboard.keypad.7";
            case 328 -> "key.keyboard.keypad.8";
            case 329 -> "key.keyboard.keypad.9";
            case 330 -> "key.keyboard.keypad.decimal";
            case 331 -> "key.keyboard.keypad.divide";
            case 332 -> "key.keyboard.keypad.multiply";
            case 333 -> "key.keyboard.keypad.subtract";
            case 334 -> "key.keyboard.keypad.add";
            case 335 -> "key.keyboard.keypad.enter";
            case 336 -> "key.keyboard.keypad.equal";
            case 340 -> "key.keyboard.left.shift";
            case 341 -> "key.keyboard.left.control";
            case 342 -> "key.keyboard.left.alt";
            case 343 -> "key.keyboard.left.win";
            case 344 -> "key.keyboard.right.shift";
            case 345 -> "key.keyboard.right.control";
            case 346 -> "key.keyboard.right.alt";
            case 347 -> "key.keyboard.right.win";
            case 348 -> "key.keyboard.menu";
            default -> "key.keyboard." + code;
        };
    }
}
