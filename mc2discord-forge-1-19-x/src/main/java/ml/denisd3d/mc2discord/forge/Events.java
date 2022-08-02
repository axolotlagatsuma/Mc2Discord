package ml.denisd3d.mc2discord.forge;

import ml.denisd3d.mc2discord.core.M2DUtils;
import ml.denisd3d.mc2discord.core.Mc2Discord;
import ml.denisd3d.mc2discord.core.entities.Advancement;
import ml.denisd3d.mc2discord.core.entities.Death;
import ml.denisd3d.mc2discord.core.entities.Player;
import ml.denisd3d.mc2discord.core.events.MinecraftEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = "mc2discord", value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Events {
    @SubscribeEvent
    public static void onMinecraftChatMessageEvent(ServerChatEvent event) {
        if (event.getPlayer() == null) {
            if (!M2DUtils.canHandleEvent())
                return;
            Mc2Discord.INSTANCE.messageManager.sendInfoMessage(event.getMessage().getString());
        } else {
            MinecraftEvents.onMinecraftChatMessageEvent(event.getMessage().getString(), new Player(event.getPlayer()
                    .getGameProfile()
                    .getName(), event.getPlayer().getDisplayName().getString(), event.getPlayer()
                    .getGameProfile()
                    .getId()));
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinEvent(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftEvents.onPlayerJoinEvent(new Player(event.getEntity().getGameProfile().getName(), event.getEntity()
                .getDisplayName()
                .getString(), event.getEntity().getGameProfile().getId()));
    }

    @SubscribeEvent
    public static void onPlayerLeaveEvent(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftEvents.onPlayerLeaveEvent(new Player(event.getEntity().getGameProfile().getName(), event.getEntity()
                .getDisplayName()
                .getString(), event.getEntity().getGameProfile().getId()));
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            MinecraftEvents.onPlayerDieEvent(
                    new Player(player.getGameProfile().getName(), player.getDisplayName()
                            .getString(), player.getGameProfile().getId()),
                    new Death(event.getSource().msgId, player.getCombatTracker()
                            .getDeathMessage()
                            .getString(), player.getCombatTracker()
                            .getCombatDuration(), Optional.ofNullable(player.getCombatTracker().getKiller())
                            .map(livingEntity -> livingEntity.getDisplayName().getString())
                            .orElse(""), Optional.ofNullable(player.getCombatTracker().getKiller())
                            .map(LivingEntity::getHealth)
                            .orElse(0.0f)));
        }
    }

    @SubscribeEvent
    public static void onAdvancementEvent(AdvancementEvent event) {
        if (event.getAdvancement().getDisplay() != null && event.getAdvancement().getDisplay().shouldAnnounceChat()) {
            MinecraftEvents.onAdvancementEvent(
                    new Player(event.getEntity().getGameProfile().getName(), event.getEntity()
                            .getDisplayName()
                            .getString(), event.getEntity().getGameProfile().getId()),
                    new Advancement(event.getAdvancement().getId().getPath(), event.getAdvancement()
                            .getChatComponent()
                            .getString(), event.getAdvancement()
                            .getDisplay()
                            .getTitle()
                            .getString(), event.getAdvancement().getDisplay().getDescription().getString())
            );
        }
    }
}
