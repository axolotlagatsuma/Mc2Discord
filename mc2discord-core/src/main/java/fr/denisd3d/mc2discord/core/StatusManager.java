package fr.denisd3d.mc2discord.core;

import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ChannelModifyRequest;
import discord4j.discordjson.possible.Possible;
import fr.denisd3d.mc2discord.core.config.StatusChannels;
import fr.denisd3d.mc2discord.core.entities.Entity;

import java.time.Duration;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class StatusManager {
    private static Timer timer;

    public static void init() {
        timer = new Timer(true);

        if (!Mc2Discord.INSTANCE.config.style.presence.message.isEmpty() && Mc2Discord.INSTANCE.config.style.presence.update != 0) {
            timer.schedule(new PresenceUpdateTask(Mc2Discord.INSTANCE.config.style.presence.message, Mc2Discord.INSTANCE.config.style.presence.type, Mc2Discord.INSTANCE.config.style.presence.link), 0, Mc2Discord.INSTANCE.config.style.presence.update * 1000);
        }

        for (StatusChannels.StatusChannel statusChannel : Mc2Discord.INSTANCE.config.statusChannels.channels) {
            if (statusChannel.channel_id.equals(M2DUtils.NIL_SNOWFLAKE) || statusChannel.update_period == 0 || (statusChannel.name_message.isEmpty() && statusChannel.topic_message.isEmpty()))
                continue;
            timer.schedule(new ChannelUpdateTask(statusChannel), 0, statusChannel.update_period * 1000);
        }
    }

    public static void stop() {
        if (timer != null)
            timer.cancel();
    }

    static class ChannelUpdateTask extends TimerTask {
        private final StatusChannels.StatusChannel statusChannel;
        private boolean shouldStopNextTimeout = false;

        public ChannelUpdateTask(StatusChannels.StatusChannel statusChannel) {
            this.statusChannel = statusChannel;
        }

        @Override
        public void run() {
            try {
                if (M2DUtils.isNotConfigured()) // Check if mod is configured
                    return;

                Mc2Discord.INSTANCE.client.rest()
                        .getChannelById(this.statusChannel.channel_id)
                        .modify(ChannelModifyRequest.builder()
                                .name(!this.statusChannel.name_message.isEmpty() ? Possible.of(Entity.replace(this.statusChannel.name_message, Collections.emptyList())) : Possible.absent())
                                .topic(!this.statusChannel.topic_message.isEmpty() ? Possible.of(Entity.replace(this.statusChannel.topic_message, Collections.emptyList())) : Possible.absent())
                                .build(), null)
                        .timeout(Duration.ofSeconds(3))
                        .doOnError(throwable -> {
                            if (throwable instanceof TimeoutException) {
                                if (this.shouldStopNextTimeout) {
                                    Mc2Discord.LOGGER.error("Seem that the channel " + this.statusChannel.channel_id + " is updated too quickly. Try increasing the update period");
                                    Mc2Discord.INSTANCE.errors.add("Seem that the channel " + this.statusChannel.channel_id + " is updated too quickly. Try increasing the update period");
                                    this.cancel();
                                } else {
                                    this.shouldStopNextTimeout = true;
                                }
                            }
                        })
                        .subscribe();
            } catch (Exception e) {
                Mc2Discord.LOGGER.error("Error while updating channel " + this.statusChannel.channel_id.asString(), e);
            }
        }
    }

    private static class PresenceUpdateTask extends TimerTask {
        private final String presence_message;
        private final String presence_type;
        private final String presence_link;

        public PresenceUpdateTask(String presence_message, String presence_type, String presence_link) {
            this.presence_message = presence_message;
            this.presence_type = presence_type;
            this.presence_link = presence_link;
        }

        @Override
        public void run() {
            try {
                if (M2DUtils.isNotConfigured()) // Check if mod is configured
                    return;

                Mc2Discord.INSTANCE.client.updatePresence(ClientPresence.online(ClientActivity.of(Activity.Type.valueOf(presence_type), Entity.replace(presence_message, Collections.emptyList()), !presence_link.isEmpty() && Activity.Type.valueOf(presence_type) == Activity.Type.STREAMING ? presence_link : null)))
                        .timeout(Duration.ofSeconds(3))
                        .doOnError(throwable -> {
                            if (throwable instanceof TimeoutException) {
                                Mc2Discord.LOGGER.error("Seem that the presence is updated too quickly. Try increasing the update period");
                                Mc2Discord.INSTANCE.errors.add("Seem that the presence is updated too quickly. Try increasing the update period");
                                this.cancel();
                            }
                        })
                        .subscribe();
            } catch (Exception e) {
                Mc2Discord.LOGGER.error("Error while updating presence", e);
            }
        }
    }
}