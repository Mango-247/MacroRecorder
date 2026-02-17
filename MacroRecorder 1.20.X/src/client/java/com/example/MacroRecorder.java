package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public class MacroRecorder {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final List<MacroFrame> recordedFrames = new ArrayList<>();
    private static final List<MacroFrame> playbackFrames = new ArrayList<>();
    private static boolean isRecording = false;
    private static boolean isPlaying = false;
    private static int playbackIndex = 0;
    private static int currentRepeat = 0;
    private static final Gson gson = new Gson();
    private static final File macroDir = new File(System.getProperty("user.home") + "/AppData/Roaming/.minecraft/macros");
    private static boolean waitingForInput = false;


    private static boolean showIndicator = MacroRecorderConfigManager.config.showIndicator;
    private static boolean loop = MacroRecorderConfigManager.config.loop;
    private static int startRecordKey = MacroRecorderConfigManager.config.startRecordKey;
    private static int stopRecordKey = MacroRecorderConfigManager.config.stopRecordKey;
    private static Map<String, Integer> macroKeybinds = MacroRecorderConfigManager.config.macroKeybinds;
    private static Map<String, Boolean> waitingForMacroBind = MacroRecorderConfigManager.config.waitingForMacroBind;


    private static boolean waitingForStartKeybind = false;
    private static boolean waitingForStopKeybind = false;
    private static boolean startKeyPressedLastTick = false;
    private static boolean stopKeyPressedLastTick = false;
    private static int skipTicks = 0;
    private static Map<String, Boolean> macroKeyPressedLastTick = new HashMap<>();

    private static boolean recordOnFirstInput = MacroRecorderConfigManager.config.recordOnFirstInput;


    private static boolean recordCamera = MacroRecorderConfigManager.config.recordCamera;
    private static boolean recordCrouch = MacroRecorderConfigManager.config.recordCrouch;
    private static boolean recordJump = MacroRecorderConfigManager.config.recordJump;
    private static boolean recordMovement = MacroRecorderConfigManager.config.recordMovement;
    private static boolean recordSprint = MacroRecorderConfigManager.config.recordSprint;
    private static boolean recordPlace = MacroRecorderConfigManager.config.recordPlace;
    private static boolean recordAttack = MacroRecorderConfigManager.config.recordAttack;
    private static boolean recordUse = MacroRecorderConfigManager.config.recordUse;
    private static boolean recordDrop = MacroRecorderConfigManager.config.recordDrop;
    private static boolean recordSwapHands = MacroRecorderConfigManager.config.recordSwapHands;

    private static boolean playbackCamera = MacroRecorderConfigManager.config.playbackCamera;
    private static boolean playbackCrouch = MacroRecorderConfigManager.config.playbackCrouch;
    private static boolean playbackJump = MacroRecorderConfigManager.config.playbackJump;
    private static boolean playbackMovement = MacroRecorderConfigManager.config.playbackMovement;
    private static boolean playbackSprint = MacroRecorderConfigManager.config.playbackSprint;
    private static boolean playbackPlace = MacroRecorderConfigManager.config.playbackPlace;
    private static boolean playbackAttack = MacroRecorderConfigManager.config.playbackAttack;
    private static boolean playbackUse = MacroRecorderConfigManager.config.playbackUse;
    private static boolean playbackDrop = MacroRecorderConfigManager.config.playbackDrop;
    private static boolean playbackSwapHands = MacroRecorderConfigManager.config.playbackSwapHands;



    private static void updateConfig() {
        MacroRecorderConfigManager.config.showIndicator = showIndicator;
        MacroRecorderConfigManager.config.loop = loop;
        MacroRecorderConfigManager.config.startRecordKey = startRecordKey;
        MacroRecorderConfigManager.config.stopRecordKey = stopRecordKey;
        MacroRecorderConfigManager.config.macroKeybinds = macroKeybinds;
        MacroRecorderConfigManager.config.waitingForMacroBind = waitingForMacroBind;
        MacroRecorderConfigManager.config.recordOnFirstInput = recordOnFirstInput;

        MacroRecorderConfigManager.config.recordCamera = recordCamera;
        MacroRecorderConfigManager.config.recordCrouch = recordCrouch;
        MacroRecorderConfigManager.config.recordJump = recordJump;
        MacroRecorderConfigManager.config.recordMovement = recordMovement;
        MacroRecorderConfigManager.config.recordSprint = recordSprint;
        MacroRecorderConfigManager.config.recordPlace = recordPlace;
        MacroRecorderConfigManager.config.recordAttack = recordAttack;
        MacroRecorderConfigManager.config.recordUse = recordUse;
        MacroRecorderConfigManager.config.recordDrop = recordDrop;
        MacroRecorderConfigManager.config.recordSwapHands = recordSwapHands;

        MacroRecorderConfigManager.config.playbackCamera = playbackCamera;
        MacroRecorderConfigManager.config.playbackCrouch = playbackCrouch;
        MacroRecorderConfigManager.config.playbackJump = playbackJump;
        MacroRecorderConfigManager.config.playbackMovement = playbackMovement;
        MacroRecorderConfigManager.config.playbackSprint = playbackSprint;
        MacroRecorderConfigManager.config.playbackPlace = playbackPlace;
        MacroRecorderConfigManager.config.playbackAttack = playbackAttack;
        MacroRecorderConfigManager.config.playbackUse = playbackUse;
        MacroRecorderConfigManager.config.playbackDrop = playbackDrop;
        MacroRecorderConfigManager.config.playbackSwapHands = playbackSwapHands;

        MacroRecorderConfigManager.save();
    }



    public static void releaseAllKeys() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.options.forwardKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.dropKey.setPressed(false);
        client.options.swapHandsKey.setPressed(false);
    }

    public static void init() {
        if (!macroDir.exists()) macroDir.mkdirs();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            long window = client.getWindow().getHandle();

            // Handle keybind setting
            if (waitingForStartKeybind || waitingForStopKeybind) {
                for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                    if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) continue;

                    if (InputUtil.isKeyPressed(window, key)) {
                        String keyName = InputUtil.fromKeyCode(key, 0).getTranslationKey().replace("key.keyboard.", "");
                        if (waitingForStartKeybind) {
                            startRecordKey = key;
                            waitingForStartKeybind = false;
                            client.player.sendMessage(Text.literal("§aStart Recording keybind set to: " + keyName), false);
                            skipTicks = 5;
                        } else {
                            stopRecordKey = key;
                            waitingForStopKeybind = false;
                            client.player.sendMessage(Text.literal("§aStop Recording keybind set to: " + keyName), false);
                            skipTicks = 5;
                        }
                        updateConfig();
                        break;
                    }
                }
            }

            for (Map.Entry<String, Boolean> entry : waitingForMacroBind.entrySet()) {
                String filename = entry.getKey();
                if (!entry.getValue()) continue;
                for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                    if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) continue;
                    if (InputUtil.isKeyPressed(window, key)) {
                        macroKeybinds.put(filename, key);
                        waitingForMacroBind.put(filename, false);
                        String keyName = InputUtil.fromKeyCode(key, 0).getTranslationKey().replace("key.keyboard.", "");
                        client.player.sendMessage(Text.literal("§aMacro '" + filename + "' bound to key: " + keyName), false);
                        skipTicks = 5;
                        updateConfig();
                        break;
                    }
                }
            }

            skipTicks --;

            for (Map.Entry<String, Integer> bind : macroKeybinds.entrySet()) {
                String name = bind.getKey();
                int key = bind.getValue();
                boolean wasPressed = macroKeyPressedLastTick.getOrDefault(name, false);
                boolean isPressed = InputUtil.isKeyPressed(window, key);

                if (isPressed && !wasPressed && skipTicks < 0 && client.currentScreen == null) {
                    File file = new File(macroDir, name + ".json");
                    try (FileReader reader = new FileReader(file)) {
                        if (isPlaying) {
                            client.player.sendMessage(Text.literal("§cCannot play while recording. Run /macrorecorder stop first."), false);
                        } else {
                            Type listType = new TypeToken<ArrayList<MacroFrame>>() {
                            }.getType();
                            List<MacroFrame> loaded = gson.fromJson(reader, listType);
                            playbackFrames.clear();
                            playbackFrames.addAll(loaded);
                            playbackIndex = 0;
                            isPlaying = true;
                            client.player.sendMessage(Text.literal("§aPlaying macro: " + name), false);
                            if (loop) {
                                client.player.sendMessage(Text.literal("§7Note: Macro is set to loop after completing."), false);
                            }
                        }
                    } catch (IOException e) {
                        client.player.sendMessage(Text.literal("§cFailed to play bound macro: " + name), false);
                        e.printStackTrace();
                    }
                }
                macroKeyPressedLastTick.put(name, isPressed);
            }


            // Handle startRecording keybind
            boolean startPressed = startRecordKey != -1 && InputUtil.isKeyPressed(window, startRecordKey);
            if (startPressed && !startKeyPressedLastTick && skipTicks < 0 && client.currentScreen == null) {
                if (isPlaying) isPlaying = false;
                recordedFrames.clear();
                if (!isRecording) {
                    isRecording = true;
                    client.player.sendMessage(Text.literal("§aRecording started. Run /macrorecorder stop to stop recording."), false);
                } else {
                    client.player.sendMessage(Text.literal("§aRecording restarted. Run /macrorecorder stop to stop recording."), false);
                }
            }
            startKeyPressedLastTick = startPressed;

            // Handle stopRecording keybind
            boolean stopPressed = startRecordKey != -1 && InputUtil.isKeyPressed(window, stopRecordKey);
            if (stopPressed && !stopKeyPressedLastTick && skipTicks < 0 && client.currentScreen == null) {
                if (isRecording) {
                    isRecording = false;
                    client.player.sendMessage(Text.literal("§eRecording stopped."), false);
                } else {
                    client.player.sendMessage(Text.literal("§cUnable to stop recording. You are not recording."), false);
                }
            }
            stopKeyPressedLastTick = stopPressed;

            // Recording logic...
            if (isRecording) {
                if (waitingForInput) {
                    if (client.options.forwardKey.isPressed() || client.options.leftKey.isPressed() || client.options.rightKey.isPressed() || client.options.backKey.isPressed() || client.options.jumpKey.isPressed() || client.options.sneakKey.isPressed() || client.options.attackKey.isPressed() || client.options.useKey.isPressed()) {
                        waitingForInput = false;
                    }
                }
                if (!waitingForInput) {
                    MacroFrame newFrame = new MacroFrame(
                            recordCamera ? client.player.getYaw() : Float.MIN_VALUE,
                            recordCamera ? client.player.getPitch() : Float.MIN_VALUE,
                            recordMovement ? client.options.forwardKey.isPressed() : null,
                            recordMovement ? client.options.leftKey.isPressed() : null,
                            recordMovement ? client.options.backKey.isPressed() : null,
                            recordMovement ? client.options.rightKey.isPressed() : null,
                            recordJump ? client.options.jumpKey.isPressed() : null,
                            recordSprint ? client.options.sprintKey.isPressed() : null,
                            recordCrouch ? client.options.sneakKey.isPressed() : null,
                            recordAttack ? client.options.attackKey.isPressed() : null,
                            recordUse ? client.options.useKey.isPressed() : null,
                            getSelectedSlotReflect(client.player),
                            recordDrop ? client.options.dropKey.isPressed() : null,
                            recordSwapHands ? client.options.swapHandsKey.isPressed() : null,
                            1
                    );

                    if (!recordedFrames.isEmpty()) {
                        MacroFrame lastFrame = recordedFrames.get(recordedFrames.size() - 1);
                        if (lastFrame.equalsWithoutRepeats(newFrame)) {
                            recordedFrames.set(recordedFrames.size() - 1, lastFrame.withIncreasedRepeats());
                        } else {
                            recordedFrames.add(newFrame);
                        }
                    } else {
                        recordedFrames.add(newFrame);
                    }
                }
            }


            // Playback logic...
            if (isPlaying) {
                if (playbackIndex >= playbackFrames.size() && !loop) {
                    isPlaying = false;
                    releaseAllKeys();
                    client.player.sendMessage(Text.literal("§ePlayback finished and loop set to false. Stopping."), false);
                    return;
                } else if (playbackIndex >= playbackFrames.size() && loop) {
                    playbackIndex = 0;
                    currentRepeat = 0;
                }

                MacroFrame frame = playbackFrames.get(playbackIndex);

                if (playbackCamera) {
                    if (frame.yaw != Float.MIN_VALUE) client.player.setYaw(frame.yaw);
                    if (frame.pitch != Float.MIN_VALUE) client.player.setPitch(frame.pitch);
                }
                if (playbackMovement) {
                    if (frame.forward != null) simulateKeyPress(client.options.forwardKey, frame.forward);
                    if (frame.left != null) simulateKeyPress(client.options.leftKey, frame.left);
                    if (frame.back != null) simulateKeyPress(client.options.backKey, frame.back);
                    if (frame.right != null) simulateKeyPress(client.options.rightKey, frame.right);
                }
                if (playbackJump && frame.jump != null) simulateKeyPress(client.options.jumpKey, frame.jump);
                if (playbackSprint && frame.sprint != null) simulateKeyPress(client.options.sprintKey, frame.sprint);
                if (playbackCrouch && frame.sneak != null) simulateKeyPress(client.options.sneakKey, frame.sneak);

                if (playbackAttack && Boolean.TRUE.equals(frame.attack)) {
                    simulateKeyPress(client.options.attackKey, true);
                    if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                        Entity target = ((EntityHitResult) client.crosshairTarget).getEntity();
                        client.interactionManager.attackEntity(client.player, target);
                    }
                    client.player.swingHand(Hand.MAIN_HAND);
                }

                if (playbackUse && frame.use != null) simulateKeyPress(client.options.useKey, frame.use);
                if (playbackDrop && Boolean.TRUE.equals(frame.dropItem)) client.player.dropSelectedItem(false);
                if (playbackSwapHands && Boolean.TRUE.equals(frame.swapOffhand)) {
                    client.getNetworkHandler().sendPacket(
                            new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.UP)
                    );
                }

                try {
                    Field field = client.player.getInventory().getClass().getDeclaredField("field_7545");
                    field.setAccessible(true);
                    field.setInt(client.player.getInventory(), frame.selectedSlot);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                currentRepeat++;
                if (currentRepeat >= frame.repeats) {
                    currentRepeat = 0;
                    playbackIndex++;
                }
            }

        });


        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (isRecording && showIndicator && client.player != null) {
                int screenWidth = client.getWindow().getScaledWidth();
                int screenHeight = client.getWindow().getScaledHeight();

                int color = 0xFFFF0000;
                String message = "Recording";

                if (waitingForInput) {
                    message = "Waiting For Input";
                    color = 0xFFFFFF00;
                }

                int lineHeight = client.textRenderer.fontHeight + 2; // Adjust line spacing as needed
                int textWidth = client.textRenderer.getWidth(message);
                int x = (screenWidth - textWidth) / 2;
                int y = screenHeight - 50;

                drawContext.drawText(client.textRenderer, Text.literal(message), x, y, 0xFFFF0000, true);
            }
        });

        registerCommands();
    }

    private static void simulateKeyPress(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
    }

    private static int getSelectedSlotReflect(ClientPlayerEntity player) {
        try {
            Field field = player.getInventory().getClass().getDeclaredField("field_7545");
            field.setAccessible(true);
            return field.getInt(player.getInventory());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static void moveItemToSlot(ItemStack desired, int targetSlot) {
        PlayerInventory inv = client.player.getInventory();
        ItemStack current = inv.getStack(targetSlot);

        if (ItemStack.areEqual(desired, current)) return;

        for (int i = 0; i < inv.size(); i++) {
            if (i == targetSlot) continue;
            ItemStack found = inv.getStack(i);
            if (ItemStack.areEqual(desired, found)) {
                inv.setStack(i, current);
                inv.setStack(targetSlot, found);
                return;
            }
        }
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("macrorecorder")
                    .then(literal("record").executes(ctx -> {
                        if (isPlaying) {
                            client.player.sendMessage(Text.literal("§cCannot play while recording. Run /macrorecorder stop first."), false);
                            return 1;
                        }
                        recordedFrames.clear();

                        if (!isRecording) {
                            isRecording = true;
                            client.player.sendMessage(Text.literal("§aRecording started. Run /macrorecorder stop to stop recording."), false);
                        } else {
                            client.player.sendMessage(Text.literal("§aRecording restarted. Run /macrorecorder stop to stop recording."), false);
                        }
                        if (recordOnFirstInput) {
                            waitingForInput = true;
                        }
                        return 1;
                    }))
                    .then(literal("stop").executes(ctx -> {
                        waitingForInput = false;
                        if (!isRecording && !isPlaying) {
                            client.player.sendMessage(Text.literal("§eThere is nothing to stop!"), false);
                        }
                        if (isRecording) {
                            isRecording = false;
                            client.player.sendMessage(Text.literal("§eRecording stopped. To test this macro run /macrorecorder test, and to save this macro run /macrorecorder save <file name>"), false);
                        }
                        if (isPlaying) {
                            isPlaying = false;
                            client.player.sendMessage(Text.literal("§ePlayback stopped. Run /macrorecorder test to test, and /macrorecorder save <file name> to save."), false);
                        }
                        return 1;
                    }))
                    .then(literal("test").executes(ctx -> {
                        if (recordedFrames.isEmpty()) {
                            client.player.sendMessage(Text.literal("§cNo macro recorded. Do /macrorecorder record to record one, and use this command to test it before saving with /macrorecorder save <file name>"), false);
                            return 0;
                        }
                        if (isRecording) {
                            client.player.sendMessage(Text.literal("§cCannot test while recording. Use /macrorecorder stop first."), false);
                            return 0;
                        }
                        playbackFrames.clear();
                        playbackFrames.addAll(recordedFrames);
                        playbackIndex = 0;
                        isPlaying = true;
                        client.player.sendMessage(Text.literal("§aTesting macro..."), false);
                        if (loop) {
                            client.player.sendMessage(Text.literal("§7Note: Macro is set to loop after completing."), false);
                        }
                        return 1;
                    }))
                    .then(literal("save")
                            .then(argument("filename", StringArgumentType.word())
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "filename");
                                        File file = new File(macroDir, name + ".json");

                                        if (recordedFrames.size() == 0) {
                                            client.player.sendMessage(Text.literal("§cRecord a macro with /macrorecorder record first."), false);
                                            return 0;
                                        }

                                        if (file.exists()) {
                                            client.player.sendMessage(Text.literal("§cMacro '" + name + "' already exists. Run /macrorecorder delete " + name + " first."), false);
                                            return 0;
                                        }

                                        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

                                        try (FileWriter writer = new FileWriter(file)) {
                                            writer.write("// Each entry in the below list is one tick, and the repeats parameter is how many times it will play that tick before moving to the next.\n\n");
                                            prettyGson.toJson(recordedFrames, writer);
                                            client.player.sendMessage(Text.literal("§aMacro saved to: " + file.getAbsolutePath()), false);
                                        } catch (IOException e) {
                                            client.player.sendMessage(Text.literal("§cFailed to save macro. Please try again. If the issue persists, try a different name, restarting Minecraft, or re-installing the mod."), false);
                                            e.printStackTrace();
                                        }

                                        return 1;
                                    })
                            )
                    )

                    .then(literal("play")
                            .then(argument("filename", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        File[] files = macroDir.listFiles((d, name) -> name.endsWith(".json"));
                                        if (files != null) {
                                            for (File f : files) {
                                                builder.suggest(f.getName().replaceFirst("\\.json$", ""));
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        if (isRecording) {
                                            client.player.sendMessage(Text.literal("§cCannot play while recording. Run /macrorecorder stop first."), false);
                                            return 0;
                                        }
                                        String filename = StringArgumentType.getString(ctx, "filename") + ".json";
                                        File file = new File(macroDir, filename);
                                        try (FileReader reader = new FileReader(file)) {
                                            Type listType = new TypeToken<ArrayList<MacroFrame>>(){}.getType();
                                            List<MacroFrame> loaded = gson.fromJson(reader, listType);
                                            playbackFrames.clear();
                                            playbackFrames.addAll(loaded);
                                            playbackIndex = 0;
                                            isPlaying = true;
                                            client.player.sendMessage(Text.literal("§aPlaying macro from " + filename), false);
                                            if (loop) {
                                                client.player.sendMessage(Text.literal("§7Note: Macro is set to loop after completing."), false);
                                            }
                                        } catch (IOException e) {
                                            client.player.sendMessage(Text.literal("§cFailed to load macro."), false);
                                            e.printStackTrace();
                                            return 0;
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("delete")
                            .then(argument("filename", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        File[] files = macroDir.listFiles((d, name) -> name.endsWith(".json"));
                                        if (files != null) {
                                            for (File f : files) {
                                                builder.suggest(f.getName().replaceFirst("\\.json$", ""));
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        String filename = StringArgumentType.getString(ctx, "filename");
                                        String fullName = filename + ".json";
                                        File file = new File(macroDir, fullName);

                                        if (!file.exists()) {
                                            client.player.sendMessage(Text.literal("§cMacro not found: " + fullName), false);
                                            return 0;
                                        }

                                        // Remove keybind using filename without .json
                                        macroKeybinds.remove(filename);

                                        if (file.delete()) {
                                            client.player.sendMessage(Text.literal("§eDeleted macro: " + fullName), false);
                                            return 1;
                                        } else {
                                            client.player.sendMessage(Text.literal("§cFailed to delete macro: " + fullName), false);
                                            return 0;
                                        }
                                    })
                            )
                    )
                    .then(literal("showRecordingIndicator")
                            .then(argument("bool", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        boolean value = getBool(ctx, "bool");
                                        showIndicator = value;
                                        updateConfig();
                                        client.player.sendMessage(Text.literal("§eRecording indicator shown set to: " + showIndicator), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("recordOnFirstInput")
                            .then(argument("bool", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        boolean value = getBool(ctx, "bool");
                                        recordOnFirstInput = value;
                                        updateConfig();
                                        client.player.sendMessage(Text.literal("§eRecord on first input: " + recordOnFirstInput), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("loop")
                            .then(argument("bool", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        boolean value = getBool(ctx, "bool");
                                        loop = value;
                                        updateConfig();
                                        client.player.sendMessage(Text.literal("§eLoop set to: " + loop), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("bind")
                            .then(literal("macro")
                                    .then(argument("filename", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                File[] files = macroDir.listFiles((d, name) -> name.endsWith(".json"));
                                                if (files != null) {
                                                    for (File f : files) {
                                                        builder.suggest(f.getName().replaceFirst("\\.json$", ""));
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(literal("record")
                                                    .executes(ctx -> {
                                                        String filename = StringArgumentType.getString(ctx, "filename");
                                                        waitingForMacroBind.put(filename, true);
                                                        client.player.sendMessage(Text.literal("Please press the key to bind macro '" + filename + "'."), false);
                                                        updateConfig();
                                                        return 1;
                                                    })
                                            )
                                            .then(literal("remove")
                                                    .executes(ctx -> {
                                                        String filename = StringArgumentType.getString(ctx, "filename");
                                                        if (macroKeybinds.containsKey(filename)) {
                                                            macroKeybinds.remove(filename);
                                                            client.player.sendMessage(Text.literal("§eRemoved macro keybind for: " + filename), false);
                                                            updateConfig();
                                                            return 1;
                                                        } else {
                                                            client.player.sendMessage(Text.literal("§cNo macro keybind found for: " + filename), false);
                                                            return 0;
                                                        }
                                                    })
                                            )
                                    )
                            )
                            .then(literal("startRecording")
                                    .then(literal("record")
                                            .executes(ctx -> {
                                                waitingForStartKeybind = true;
                                                client.player.sendMessage(Text.literal("Please press the key you want to bind 'startRecording' to."), false);
                                                updateConfig();
                                                return 1;
                                            }))
                                    .then(literal("remove")
                                            .executes(ctx -> {
                                                startRecordKey = -1;
                                                updateConfig();
                                                client.player.sendMessage(Text.literal("§cStart Recording keybind removed."), false);
                                                updateConfig();
                                                return 1;
                                            })))
                            .then(literal("stopRecording")
                                    .then(literal("record")
                                            .executes(ctx -> {
                                                waitingForStopKeybind = true;
                                                client.player.sendMessage(Text.literal("Please press the key you want to bind 'stopRecording' to."), false);
                                                updateConfig();
                                                return 1;
                                            }))
                                    .then(literal("remove")
                                            .executes(ctx -> {
                                                stopRecordKey = -1;
                                                updateConfig();
                                                client.player.sendMessage(Text.literal("§cStop Recording keybind removed."), false);
                                                return 1;
                                            }))))
                    .then(literal("recordSettings")
                            .then(argument("setting", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        String[] recordOptions = new String[]{
                                                "recordCamera",
                                                "recordCrouch", "recordJump", "recordMovement", "recordSprint",
                                                "recordPlace", "recordAttack", "recordUse", "recordDrop", "recordSwapHands"
                                        };
                                        String input = builder.getRemaining().toLowerCase();
                                        for (String option : recordOptions) {
                                            if (option.toLowerCase().startsWith(input)) {
                                                builder.suggest(option);
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(argument("bool", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                String setting = StringArgumentType.getString(ctx, "setting");
                                                boolean value = getBool(ctx, "bool");
                                                switch (setting) {
                                                    case "recordCamera": recordCamera = value; break;
                                                    case "recordCrouch": recordCrouch = value; break;
                                                    case "recordJump": recordJump = value; break;
                                                    case "recordMovement": recordMovement = value; break;
                                                    case "recordSprint": recordSprint = value; break;
                                                    case "recordPlace": recordPlace = value; break;
                                                    case "recordAttack": recordAttack = value; break;
                                                    case "recordUse": recordUse = value; break;
                                                    case "recordDrop": recordDrop = value; break;
                                                    case "recordSwapHands": recordSwapHands = value; break;
                                                    default:
                                                        client.player.sendMessage(Text.literal("§cUnknown record setting: " + setting), false);
                                                        return 0;
                                                }
                                                updateConfig();
                                                client.player.sendMessage(Text.literal("§a" + setting + " set to: " + value), false);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(literal("playbackSettings")
                            .then(argument("setting", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        String[] playbackOptions = new String[]{
                                                "playbackCamera",
                                                "playbackCrouch", "playbackJump", "playbackMovement", "playbackSprint",
                                                "playbackPlace", "playbackAttack", "playbackUse", "playbackDrop", "playbackSwapHands"
                                        };
                                        String input = builder.getRemaining().toLowerCase();
                                        for (String option : playbackOptions) {
                                            if (option.toLowerCase().startsWith(input)) {
                                                builder.suggest(option);
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(argument("bool", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                String setting = StringArgumentType.getString(ctx, "setting");
                                                boolean value = getBool(ctx, "bool");
                                                switch (setting) {
                                                    case "playbackCamera": playbackCamera = value; break;
                                                    case "playbackCrouch": playbackCrouch = value; break;
                                                    case "playbackJump": playbackJump = value; break;
                                                    case "playbackMovement": playbackMovement = value; break;
                                                    case "playbackSprint": playbackSprint = value; break;
                                                    case "playbackPlace": playbackPlace = value; break;
                                                    case "playbackAttack": playbackAttack = value; break;
                                                    case "playbackUse": playbackUse = value; break;
                                                    case "playbackDrop": playbackDrop = value; break;
                                                    case "playbackSwapHands": playbackSwapHands = value; break;
                                                    default:
                                                        client.player.sendMessage(Text.literal("§cUnknown playback setting: " + setting), false);
                                                        return 0;
                                                }
                                                updateConfig();
                                                client.player.sendMessage(Text.literal("§a" + setting + " set to: " + value), false);
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(literal("help").executes(ctx -> {
                        client.player.sendMessage(Text.literal("§6§lMacroRecorder Help"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder record§7 - Start recording a macro"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder test§7 - Plays your last recording without you having to save it first."), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder stop§7 - Stop recording or playback"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder save <name>§7 - Save the current recording to .minecraft/macros"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder play <name>§7 - Play a saved macro"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder delete <name>§7 - Delete a saved macro"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder showRecordingIndicator <true|false>§7 - Toggle recording indicator"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder loop <true|false>§7 - If macros should loop after finishing"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder bind startRecording record§7 - Bind a key to start recording"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder bind startRecording remove§7 - Unbind the startRecording key"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder bind stopRecording record§7 - Bind a key to stop recording"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder bind stopRecording remove§7 - Unbind the stopRecording key"), false);
                        client.player.sendMessage(Text.literal("§a/macrorecorder help§7 - Show this help message"), false);
                        return 1;
                    }))
            );
        });
    }


    static class MacroFrame {
        final float yaw, pitch;
        final Boolean forward, left, back, right, jump, sprint, sneak, attack, use, dropItem, swapOffhand;
        final int selectedSlot, repeats;

        MacroFrame(float yaw, float pitch, Boolean forward, Boolean left, Boolean back, Boolean right,
                   Boolean jump, Boolean sprint, Boolean sneak, Boolean attack, Boolean use, int selectedSlot,
                   Boolean dropItem, Boolean swapOffhand, int repeats) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.forward = forward;
            this.left = left;
            this.back = back;
            this.right = right;
            this.jump = jump;
            this.sprint = sprint;
            this.sneak = sneak;
            this.attack = attack;
            this.use = use;
            this.selectedSlot = selectedSlot;
            this.dropItem = dropItem;
            this.swapOffhand = swapOffhand;
            this.repeats = repeats;
        }

        private boolean isEqualNullableBool(Boolean a, Boolean b) {
            if (a == null || b == null) return true;
            return a.equals(b);
        }

        private boolean isEqualFloat(float a, float b) {
            if (Float.isNaN(a) || Float.isNaN(b)) return true;
            return Float.compare(a, b) == 0;
        }

        boolean equalsWithoutRepeats(MacroFrame other) {
            if (other == null) return false;
            return isEqualFloat(yaw, other.yaw) &&
                    isEqualFloat(pitch, other.pitch) &&
                    isEqualNullableBool(forward, other.forward) &&
                    isEqualNullableBool(left, other.left) &&
                    isEqualNullableBool(back, other.back) &&
                    isEqualNullableBool(right, other.right) &&
                    isEqualNullableBool(jump, other.jump) &&
                    isEqualNullableBool(sprint, other.sprint) &&
                    isEqualNullableBool(sneak, other.sneak) &&
                    isEqualNullableBool(attack, other.attack) &&
                    isEqualNullableBool(use, other.use) &&
                    selectedSlot == other.selectedSlot &&
                    isEqualNullableBool(dropItem, other.dropItem) &&
                    isEqualNullableBool(swapOffhand, other.swapOffhand);
        }

        MacroFrame withIncreasedRepeats() {
            return new MacroFrame(yaw, pitch, forward, left, back, right, jump, sprint, sneak, attack, use,
                    selectedSlot, dropItem, swapOffhand, repeats + 1);
        }
    }


}
