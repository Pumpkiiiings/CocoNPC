package com.pumpkings.coconpc.action;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.action.processor.impl.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ActionsManager {

    private final CocoNPC plugin;
    private final Map<String, ActionProcessor> processors = new HashMap<>();

    public ActionsManager(CocoNPC plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    public void register(ActionProcessor processor) {
        processors.put(processor.tag().toLowerCase(), processor);
    }

    public java.util.Set<String> getRegisteredTags() {
        return java.util.Collections.unmodifiableSet(processors.keySet());
    }

    public static java.util.Set<String> getConditionTags() {
        return java.util.Set.of("[require_permission]", "[require_money]", "[cooldown]");
    }

    public static java.util.Set<String> getInterceptedTags() {
        return java.util.Set.of("[title]", "[subtitle]");
    }

    private void registerDefaults() {
        register(new MessageProcessor(plugin));
        register(new ConsoleProcessor(plugin));
        register(new PlayerCommandProcessor(plugin));
        register(new ChatProcessor(plugin));
        register(new BroadcastProcessor(plugin));
        register(new GiveExpProcessor());
        register(new TakeExpProcessor());
        register(new ConnectProcessor(plugin));
        register(new SoundProcessor());
        register(new GiveItemProcessor());
        register(new TakeItemProcessor());
        register(new TeleportProcessor());
        register(new ActionBarProcessor(plugin));
        register(new PotionProcessor());
        register(new HealProcessor());
        register(new FeedProcessor());
    }

    public void runActions(final Player player, final List<String> actions, final String npcId) {
        if (actions == null || actions.isEmpty()) return;

        if (plugin.getConfigManager().isBedrockBlocked()
                && plugin.getUtils().isFloodgatePlayer(player.getUniqueId())) {
            if (plugin.getConfigManager().isBedrockNotified()) {
                Message.BEDROCK_UNSUPPORTED.send(plugin, player);
            }
            return;
        }

        player.getScheduler().runDelayed(plugin, task -> {
            String title = null;
            String subtitle = null;

            for (String raw : actions) {
                if (raw == null || raw.isBlank()) continue;
                int spaceIdx = raw.indexOf(' ');
                String tag  = (spaceIdx == -1 ? raw : raw.substring(0, spaceIdx)).toLowerCase();
                String data = (spaceIdx == -1 ? "" : raw.substring(spaceIdx + 1));
                
                if (tag.equals("[title]")) {
                    title = plugin.getUtils().color(data);
                    continue;
                } else if (tag.equals("[subtitle]")) {
                    subtitle = plugin.getUtils().color(data);
                    continue;
                }

                if (!dispatch(player, tag, data, npcId)) {
                    break;
                }
            }
            
            if (title != null || subtitle != null) {
                net.kyori.adventure.text.Component titleComp = title != null ? MiniMessage.miniMessage().deserialize(title) : net.kyori.adventure.text.Component.empty();
                net.kyori.adventure.text.Component subtitleComp = subtitle != null ? MiniMessage.miniMessage().deserialize(subtitle) : net.kyori.adventure.text.Component.empty();
                player.showTitle(Title.title(titleComp, subtitleComp, Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(plugin.getConfigManager().getTitleDurationSeconds()), java.time.Duration.ZERO)));
            }
        }, null, 1L);
    }

    private boolean dispatch(Player player, String tag, String data, String npcId) {
        String safeName = plugin.getUtils().sanitizeCommandArgument(player.getName());
        data = data.replace("<player>", safeName)
                   .replace("%player_name%", safeName);

        if (tag.equals("[require_permission]")) {
            return player.hasPermission(data);
        }

        if (tag.equals("[require_money]")) {
            boolean strict = plugin.getConfigManager().isRequireMoneyStrict();

            double amount;
            try {
                amount = Double.parseDouble(data);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[CocoNPC] [require_money] has a non-numeric amount: \"" + data + "\"");
                return !strict;
            }

            if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                plugin.getLogger().warning("[CocoNPC] [require_money] needs PlaceholderAPI + Vault, which is not enabled."
                        + (strict ? " Blocking the action." : " Letting the action through (require-money-strict is false)."));
                return !strict;
            }

            String balStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance%");
            double balance;
            try {
                balance = Double.parseDouble(balStr);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[CocoNPC] [require_money] could not read a balance (got \"" + balStr
                        + "\"). Is the Vault expansion installed?"
                        + (strict ? " Blocking the action." : " Letting the action through (require-money-strict is false)."));
                return !strict;
            }

            return balance >= amount;
        }

        if (tag.equals("[cooldown]")) {
            return plugin.getCooldownManager().tryCooldown(player, npcId, data);
        }

        ActionProcessor processor = processors.get(tag);
        if (processor != null) {
            processor.execute(player, data);
        } else {
            plugin.getLogger().warning("[CocoNPC] Unknown action tag: \"" + tag + "\" — skipping.");
        }
        return true;
    }

    public void clear(Player player) {
        // No longer needed, as we process titles synchronously in the loop
    }

}


