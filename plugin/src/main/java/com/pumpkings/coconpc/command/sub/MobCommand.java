package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.NpcCommand;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.mob.CustomMob;
import com.pumpkings.coconpc.core.mob.MobDefinition;
import com.pumpkings.coconpc.core.mob.MobTextureService;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import com.pumpkings.coconpc.menu.MobEditorMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /coconpc mob ...} — manages real .bbmodel mobs loaded from
 * {@code plugins/CocoNPC/mobs}.
 */
public class MobCommand extends SubCommand {

    public MobCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Message.MOB_USAGE.send(plugin, player);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> list(player);
            case "spawn" -> spawn(player, args);
            case "remove", "delete" -> remove(player, args);
            case "reload" -> reload(player);
            case "info" -> info(player, args);
            case "animate" -> animate(player, args);
            case "stopanim", "stop" -> stopAnimation(player, args);
            case "tp", "teleport" -> teleport(player, args);
            case "edit", "menu" -> edit(player, args);
            case "action", "actions" -> action(player, args);
            default -> Message.MOB_USAGE.send(plugin, player);
        }
    }

    private void list(Player player) {
        List<MobDefinition> definitions = new ArrayList<>(plugin.getMobRegistry().getDefinitions());
        if (definitions.isEmpty()) {
            Message.MOB_LIST_EMPTY.send(plugin, player);
            return;
        }

        definitions.sort(Comparator.comparing(MobDefinition::getId));
        Message.MOB_LIST_HEADER.send(plugin, player);

        MobTextureService textures = plugin.getMobTextureService();
        for (MobDefinition definition : definitions) {
            String status = status(definition, textures);
            Message.MOB_LIST_ENTRY.send(plugin, player,
                    "{id}", definition.getId(),
                    "{driver}", definition.getDriverType().name(),
                    "{cubes}", String.valueOf(cubeCount(definition)),
                    "{alive}", String.valueOf(plugin.getMobRegistry()
                            .countMobs(definition.getId())),
                    "{status}", status);
        }
    }

    private void spawn(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_SPAWN_USAGE.send(plugin, player);
            return;
        }

        String id = args[2];
        MobDefinition definition = plugin.getMobRegistry().getDefinition(id);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", id);
            return;
        }

        Location location = playerFacingLocation(player);
        Message.MOB_SPAWNING.send(plugin, player, "{id}", definition.getId());

        CompletableFuture<Void> ready = plugin.getMobTextureService().whenReady(definition.getId());
        ready.whenComplete((ignored, error) -> {
            if (error != null) {
                sendLater(player, Message.MOB_TEXTURE_FAILED,
                        "{id}", definition.getId(),
                        "{error}", rootMessage(error));
                return;
            }

            runAt(location, () -> {
                MobDefinition current = plugin.getMobRegistry().getDefinition(definition.getId());
                if (current == null) {
                    sendLater(player, Message.MOB_NOT_FOUND, "{id}", definition.getId());
                    return;
                }

                try {
                    CustomMob mob = plugin.getMobRegistry().spawn(current, location);
                    if (mob == null) {
                        sendLater(player, Message.MOB_SPAWN_FAILED,
                                "{id}", definition.getId(),
                                "{error}", "unknown error");
                    } else {
                        sendLater(player, Message.MOB_SPAWNED, "{id}", definition.getId());
                    }
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("[CocoNPC] Could not spawn mob '"
                            + definition.getId() + "': " + exception.getMessage());
                    sendLater(player, Message.MOB_SPAWN_FAILED,
                            "{id}", definition.getId(),
                            "{error}", rootMessage(exception));
                }
            });
        });
    }

    private void remove(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_REMOVE_USAGE.send(plugin, player);
            return;
        }

        String id = args[2];
        if (id.equalsIgnoreCase("all")) {
            int amount = plugin.getMobRegistry().removeMobs(null);
            Message.MOB_REMOVED_ALL.send(plugin, player, "{amount}", String.valueOf(amount));
            return;
        }

        MobDefinition definition = plugin.getMobRegistry().getDefinition(id);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", id);
            return;
        }

        int amount = plugin.getMobRegistry().removeMobs(definition.getId());
        Message.MOB_REMOVED.send(plugin, player,
                "{amount}", String.valueOf(amount),
                "{id}", definition.getId());
    }

    private void reload(Player player) {
        plugin.reloadMobs();
        Message.MOB_RELOADED.send(plugin, player,
                "{amount}", String.valueOf(plugin.getMobRegistry().getDefinitionCount()));
    }

    private void animate(Player player, String[] args) {
        if (args.length < 4) {
            Message.MOB_ANIMATE_USAGE.send(plugin, player);
            return;
        }

        String target = args[2];
        String animation = args[3];
        boolean loop = args.length > 4 && isLoopToken(args[4]);

        List<CustomMob> mobs = resolveTargets(player, target);
        if (mobs.isEmpty()) {
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", target);
            return;
        }

        int played = 0;
        String missingMob = null;
        for (CustomMob mob : mobs) {
            if (!mob.hasCustomAnimation(animation)) {
                missingMob = mob.getDefinition().getId();
                continue;
            }
            if (mob.playCustomAnimation(animation, loop)) {
                played++;
            }
        }

        if (played > 0) {
            Message.MOB_ANIMATE_SUCCESS.send(plugin, player,
                    "{animation}", animation,
                    "{amount}", String.valueOf(played));
        } else if (missingMob != null) {
            Message.MOB_ANIMATION_UNKNOWN.send(plugin, player,
                    "{id}", missingMob,
                    "{animation}", animation);
        } else {
            Message.MOB_ANIMATE_FAILED.send(plugin, player, "{animation}", animation);
        }
    }

    private void stopAnimation(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_STOPANIM_USAGE.send(plugin, player);
            return;
        }

        String target = args[2];
        List<CustomMob> mobs = resolveTargets(player, target);
        if (mobs.isEmpty()) {
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", target);
            return;
        }

        int stopped = 0;
        for (CustomMob mob : mobs) {
            if (mob.stopCustomAnimation()) {
                stopped++;
            }
        }

        if (stopped > 0) {
            Message.MOB_STOPANIM_SUCCESS.send(plugin, player,
                    "{amount}", String.valueOf(stopped));
        } else {
            Message.MOB_STOPANIM_NONE.send(plugin, player, "{id}", target);
        }
    }

    private void teleport(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_TP_USAGE.send(plugin, player);
            return;
        }

        String target = args[2];
        CustomMob mob = resolveNearest(player, target);
        if (mob == null) {
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", target);
            return;
        }

        String id = mob.getDefinition().getId();
        if (mob.teleportTo(player.getLocation())) {
            Message.MOB_TP_SUCCESS.send(plugin, player, "{id}", id);
        } else {
            Message.MOB_TP_FAILED.send(plugin, player, "{id}", id);
        }
    }

    private void edit(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_EDIT_USAGE.send(plugin, player);
            return;
        }

        if (!MobEditorMenu.canEdit(player)) {
            Message.NO_PERMISSION.send(plugin, player);
            return;
        }

        CustomMob mob = resolveNearest(player, args[2]);
        if (mob == null) {
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", args[2]);
            return;
        }

        new MobEditorMenu(plugin, mob).open(player);
        Message.MOB_EDITOR_OPENED.send(plugin, player,
                "{id}", mob.getDefinition().getId());
    }

    private void action(Player player, String[] args) {
        if (args.length < 4) {
            sendMobActionHelp(player);
            return;
        }

        MobDefinition definition = resolveDefinition(player, args[2]);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", args[2]);
            return;
        }

        String sub = args[3].toLowerCase();
        String id = definition.getId();
        List<String> currentActions = new ArrayList<>(definition.getActions());

        switch (sub) {
            case "list" -> {
                if (currentActions.isEmpty()) {
                    Message.MOB_ACTION_LIST_EMPTY.send(plugin, player);
                    return;
                }
                Message.MOB_ACTION_LIST_HEADER.send(plugin, player, "{id}", id);
                for (int i = 0; i < currentActions.size(); i++) {
                    Message.MOB_ACTION_LIST_ENTRY.send(plugin, player,
                            "{index}", String.valueOf(i + 1),
                            "{action}", currentActions.get(i));
                }
            }
            case "clear" -> {
                if (plugin.getMobRegistry().saveActions(id, List.of())) {
                    Message.MOB_ACTION_CLEARED.send(plugin, player, "{id}", id);
                } else {
                    Message.MOB_SAVE_FAILED.send(plugin, player, "{id}", id);
                }
            }
            case "remove", "del", "delete" -> {
                if (args.length < 5) {
                    Message.MOB_ACTION_HELP_REMOVE.send(plugin, player);
                    return;
                }
                try {
                    int index = Integer.parseInt(args[4]);
                    if (index < 1 || index > currentActions.size()) {
                        Message.MOB_ACTION_INVALID_INDEX.send(plugin, player,
                                "{index}", args[4]);
                        return;
                    }
                    currentActions.remove(index - 1);
                    if (plugin.getMobRegistry().saveActions(id, currentActions)) {
                        Message.MOB_ACTION_REMOVED.send(plugin, player,
                                "{index}", String.valueOf(index),
                                "{id}", id);
                    } else {
                        Message.MOB_SAVE_FAILED.send(plugin, player, "{id}", id);
                    }
                } catch (NumberFormatException exception) {
                    Message.INVALID_NUMBER.send(plugin, player, "{value}", args[4]);
                }
            }
            case "add" -> {
                if (args.length < 5) {
                    Message.MOB_ACTION_HELP_ADD.send(plugin, player);
                    return;
                }

                String type = args[4].toLowerCase();
                if (type.startsWith("[") && type.endsWith("]")) {
                    type = type.substring(1, type.length() - 1);
                }
                if (type.equals("connect")) type = "server";

                if (!NpcCommand.ACTION_TYPES.contains(type)) {
                    Message.MOB_ACTION_INVALID_TYPE.send(plugin, player, "{type}", args[4]);
                    return;
                }

                String rest = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                String fullAction = type.equals("animate")
                        ? "[mob_animate] " + id + " " + rest
                        : "[" + type + "] " + rest;
                currentActions.add(fullAction);

                if (plugin.getMobRegistry().saveActions(id, currentActions)) {
                    Message.MOB_ACTION_ADDED.send(plugin, player,
                            "{id}", id,
                            "{index}", String.valueOf(currentActions.size()));
                } else {
                    Message.MOB_SAVE_FAILED.send(plugin, player, "{id}", id);
                }
            }
            default -> sendMobActionHelp(player);
        }
    }

    private void sendMobActionHelp(Player player) {
        Message.MOB_ACTION_HELP_HEADER.send(plugin, player);
        Message.MOB_ACTION_HELP_LIST.send(plugin, player);
        Message.MOB_ACTION_HELP_ADD.send(plugin, player);
        Message.MOB_ACTION_HELP_REMOVE.send(plugin, player);
        Message.MOB_ACTION_HELP_CLEAR.send(plugin, player);
    }

    private MobDefinition resolveDefinition(Player player, String target) {
        if (target == null || target.isBlank()) return null;

        if (target.equalsIgnoreCase("nearest")) {
            CustomMob mob = plugin.getMobRegistry().getNearest(player.getLocation());
            return mob != null ? mob.getDefinition() : null;
        }

        if (target.startsWith("#")) {
            try {
                CustomMob mob = plugin.getMobRegistry()
                        .getByInstance(java.util.UUID.fromString(target.substring(1)));
                return mob != null ? mob.getDefinition() : null;
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        return plugin.getMobRegistry().getDefinition(target);
    }

    private List<CustomMob> resolveTargets(Player player, String target) {
        if (target == null || target.isBlank()) return List.of();

        if (target.equalsIgnoreCase("nearest")) {
            CustomMob nearest = plugin.getMobRegistry().getNearest(player.getLocation());
            return nearest != null ? List.of(nearest) : List.of();
        }

        if (target.startsWith("#")) {
            try {
                CustomMob mob = plugin.getMobRegistry()
                        .getByInstance(java.util.UUID.fromString(target.substring(1)));
                return mob != null ? List.of(mob) : List.of();
            } catch (IllegalArgumentException exception) {
                return List.of();
            }
        }

        MobDefinition definition = plugin.getMobRegistry().getDefinition(target);
        if (definition == null) return List.of();

        List<CustomMob> matches = new ArrayList<>();
        for (CustomMob mob : plugin.getMobRegistry().getMobs()) {
            if (mob.getDefinition().getId().equals(definition.getId())) {
                matches.add(mob);
            }
        }
        return matches;
    }

    private CustomMob resolveNearest(Player player, String target) {
        List<CustomMob> candidates = resolveTargets(player, target);
        if (candidates.isEmpty()) return null;

        Location origin = player.getLocation();
        CustomMob nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (CustomMob mob : candidates) {
            Location location = mob.getLocation();
            if (location == null || location.getWorld() != origin.getWorld()) continue;

            double distance = location.distanceSquared(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = mob;
            }
        }
        return nearest;
    }

    private static boolean isLoopToken(String token) {
        return token.equalsIgnoreCase("loop")
                || token.equalsIgnoreCase("true")
                || token.equalsIgnoreCase("yes")
                || token.equals("1");
    }

    private void info(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_INFO_USAGE.send(plugin, player);
            return;
        }

        MobDefinition definition = plugin.getMobRegistry().getDefinition(args[2]);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", args[2]);
            return;
        }

        BbModel model = definition.getModel();
        Message.MOB_INFO_HEADER.send(plugin, player, "{id}", definition.getId());
        line(player, "Driver", definition.getDriverType().name());
        line(player, "Model", model != null ? model.getName() : "unknown");
        line(player, "Cubes", String.valueOf(cubeCount(definition)));
        line(player, "Animations", String.valueOf(model != null
                ? model.getAnimations().size() : 0));
        line(player, "Texture mode", definition.getTextureMode().name());
        line(player, "AI", String.valueOf(definition.isAiEnabled()));
        line(player, "Gravity", String.valueOf(definition.isGravity()));
        line(player, "Collidable", String.valueOf(definition.isCollidable()));
        line(player, "Invulnerable", String.valueOf(definition.isInvulnerable()));
        line(player, "Marker", String.valueOf(definition.isMarker()));
        line(player, "Scale", String.valueOf(definition.getScale()));
        line(player, "Yaw offset", String.valueOf(definition.getYawOffset()));
        line(player, "Actions", String.valueOf(definition.getActions().size()));
        line(player, "Cooldown", definition.getInteractionCooldown() + "s");
        line(player, "Default animation", definition.getDefaultAnimation() != null
                ? definition.getDefaultAnimation() : "none");
        line(player, "Textures", status(definition, plugin.getMobTextureService()));
        line(player, "Spawned", String.valueOf(plugin.getMobRegistry()
                .countMobs(definition.getId())));
    }

    private void line(Player player, String key, String value) {
        Message.MOB_INFO_LINE.send(plugin, player, "{key}", key, "{value}", value);
    }

    private String status(MobDefinition definition, MobTextureService textures) {
        if (textures == null) return Message.MOB_TEXTURES_GENERATING.get(plugin);

        String error = textures.getError(definition.getId());
        if (error != null) {
            return Message.MOB_TEXTURES_FAILED.get(plugin);
        }
        if (textures.isReady(definition.getId())) {
            return Message.MOB_TEXTURES_READY.get(plugin);
        }
        return Message.MOB_TEXTURES_GENERATING.get(plugin);
    }

    private static int cubeCount(MobDefinition definition) {
        BbModel model = definition.getModel();
        if (model == null) return 0;

        int cubes = 0;
        for (BbModel.Bone bone : model.getBones()) {
            cubes += bone.getCubes().size();
        }
        return cubes;
    }

    private void runAt(Location location, Runnable action) {
        try {
            Bukkit.getRegionScheduler().run(plugin, location, task -> action.run());
        } catch (RuntimeException exception) {
            action.run();
        }
    }

    private void sendLater(Player player, Message message, String... replacements) {
        if (player == null) return;
        try {
            player.getScheduler().run(plugin, task -> message.send(plugin, player, replacements), null);
        } catch (RuntimeException exception) {
            message.send(plugin, player, replacements);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}