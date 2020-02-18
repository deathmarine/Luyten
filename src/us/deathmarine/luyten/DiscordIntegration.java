package us.deathmarine.luyten;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import club.minnced.discord.rpc.DiscordUser;

public class DiscordIntegration {
    private static DiscordRPC lib;
    private static Thread thread;

    public static void init() {
//        System.out.println("Init");

        if (!isEnabled()) {
//            System.out.println("Init stop A");
            stopRPC();
            return;
        }

        if (thread != null) {
            stopRPC();
        }

        lib = DiscordRPC.INSTANCE;
        String applicationId = "445128415347998720";
        String steamId = "";
        DiscordEventHandlers handlers = new DiscordEventHandlers();

        handlers.ready = new DiscordEventHandlers.OnReady() {
            @Override
            public void accept(DiscordUser user) {
//                System.out.println("Discord RPC initialized");
                updateRPC(null, null);
            }
        };

        lib.Discord_Initialize(applicationId, handlers, true, steamId);

        if (thread == null || !thread.isAlive()) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        lib.Discord_RunCallbacks();

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }, "RPC-Callback-Handler");
            thread.start();
        }
    }

    private static boolean isEnabled() {
        return Luyten.mainWindowRef.get().luytenPrefs.isDiscordIntegrationEnabled();
    }

    public static void updateRPC(String fileName, String shownClass) {
        if (!isEnabled()) {
//            System.out.println("Update stop A");
            stopRPC();
            return;
        }
//        System.out.println("Update " + fileName + " " + shownClass);

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;

        presence.details = fileName != null ? fileName : "Idle";
        presence.state = shownClass != null ? "Decompiling " + shownClass : "";
        presence.largeImageKey = "luyten";

        lib.Discord_UpdatePresence(presence);
    }

    public static void stopRPC() {
        if (thread != null) {
            thread.stop();
            thread = null;
            lib.Discord_ClearPresence();
            lib.Discord_Shutdown();
//            System.out.println("Stopping discord integration");
        }
    }

}
