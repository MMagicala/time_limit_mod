package time_limit_mod;

import basemod.*;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PreUpdateSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpireInitializer
public class TimeLimitMod implements PostInitializeSubscriber {
    private static SpireConfig config;
    private static final String TIME_LEFT_KEY = "timeLimit";
    private static float timeLeft;
    private static long lastRecordedTime;

    public TimeLimitMod() {
        System.out.println("Time Limit Mod initialized");
    }

    public static void initialize() {
        BaseMod.subscribe(new TimeLimitMod());
    }

    @Override
    public void receivePostInitialize() {
        // Define default properties
        Properties properties = new Properties();
        properties.setProperty(TIME_LEFT_KEY, Integer.toString(30));

        // Try to load a config file. If not found, use the default properties
        try {
            config = new SpireConfig("TimeLimitMod", "config", properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SpirePatch(
            clz = EndTurnButton.class,
            method = "enable"
    )
    public static class StartTurnPatch {
        @SpirePostfixPatch
        public static void Postfix(EndTurnButton __instance) {
            timeLeft = config.getInt(TIME_LEFT_KEY);
            lastRecordedTime = System.nanoTime();
        }
    }

    @SpirePatch(
            clz = OverlayMenu.class,
            method = "update"
    )
    public static class TimerUpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(OverlayMenu __instance) {
            if (__instance.endTurnButton.enabled) {
                // Record time elapsed
                long currentTime = System.nanoTime();
                float secondsPassed = (float)(currentTime - lastRecordedTime) / TimeUnit.SECONDS.toNanos(1);
                // Only decrement timer if we are viewing the room
                if(!AbstractDungeon.isScreenUp) {
                    timeLeft -= secondsPassed;
                }
                if (timeLeft <= 0) {
                    // Time up! End turn
                    AbstractDungeon.overlayMenu.endTurnButton.disable(true);
                }
                lastRecordedTime = currentTime;
            }
        }
    }


    @SpirePatch(
            clz = OverlayMenu.class,
            method = "render"
    )
    public static class TimerRenderPatch {
        @SpirePostfixPatch
        public static void Postfix(OverlayMenu __instance, SpriteBatch sb) {
            if (__instance.endTurnButton.enabled) {
                Color color = timeLeft < 5f ? Color.RED : Color.WHITE;
                // Use ReflectionHacks to get x and y of the "End Turn" button, and apply deltas
                float x = (float)ReflectionHacks.getPrivate(__instance.endTurnButton, EndTurnButton.class, "current_x");
                float y = (float)ReflectionHacks.getPrivate(__instance.endTurnButton, EndTurnButton.class, "current_y");
                Hitbox hb = (Hitbox)ReflectionHacks.getPrivate(__instance.endTurnButton, EndTurnButton.class, "hb");
                float yDelta = -hb.height/2 - 20;
                y += yDelta;
                FontHelper.renderFontCentered(sb, FontHelper.bannerFont, Long.toString(Math.round(Math.ceil(timeLeft))),
                        x, y, color, 2);
            }
        }
    }
}