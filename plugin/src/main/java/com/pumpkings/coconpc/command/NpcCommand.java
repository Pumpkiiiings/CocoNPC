package com.pumpkings.coconpc.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.NpcPose;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.stream.Collectors;

import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.command.sub.*;
import net.kyori.adventure.text.minimessage.MiniMessage;

@NullMarked
public class NpcCommand implements BasicCommand {

    private final CocoNPC plugin;
    private final Map<String, SubCommand> handlers = new LinkedHashMap<>();

    public static final List<String> ACTION_TYPES = List.of(
            "message", "console", "command", "player", "chat", "broadcast",
            "give_exp", "take_exp", "server", "title", "subtitle",
            "sound", "teleport", "actionbar", "potion", "heal", "feed",
            "give_item", "take_item", "animate", "mob_animate",
            "require_permission", "require_money", "cooldown");

    public static final Map<String, String> PERMISSIONS = Map.ofEntries(
            Map.entry("spawn",        "CocoNPC.spawn"),
            Map.entry("create",       "CocoNPC.spawn"),
            Map.entry("list",         "CocoNPC.list"),
            Map.entry("tp",           "CocoNPC.tp"),
            Map.entry("teleport",     "CocoNPC.tp"),
            Map.entry("tphere",       "CocoNPC.tphere"),
            Map.entry("delete",       "CocoNPC.npc.delete"),
            Map.entry("edit",         "CocoNPC.npc.edit"),
            Map.entry("action",       "CocoNPC.action"),
            Map.entry("pose",         "CocoNPC.pose"),
            Map.entry("resize",       "CocoNPC.resize"),
            Map.entry("item",         "CocoNPC.item"),
            Map.entry("equip",        "CocoNPC.item"),
            Map.entry("skin",         "CocoNPC.skin"),
            Map.entry("clone",        "CocoNPC.clone"),
            Map.entry("viewdistance", "CocoNPC.viewdistance"),
            Map.entry("setkey",       "CocoNPC.admin"),
            Map.entry("preload",      "CocoNPC.reload"),
            Map.entry("reload",       "CocoNPC.reload"),
            Map.entry("animate",      "CocoNPC.animate"),
            Map.entry("animation_stop","CocoNPC.animate"),
            Map.entry("mob",          "CocoNPC.mob")
    );

    public NpcCommand(CocoNPC plugin) {
        this.plugin = plugin;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put("reload",       new AdminCommand(plugin));
        handlers.put("preload",      new AdminCommand(plugin));
        handlers.put("create",       new SpawnCommand(plugin));
        handlers.put("spawn",        new SpawnCommand(plugin));
        handlers.put("list",         new ListCommand(plugin));
        handlers.put("tp",           new TeleportCommand(plugin));
        handlers.put("teleport",     new TeleportCommand(plugin));
        handlers.put("tphere",       new TeleportCommand(plugin));
        handlers.put("delete",       new DeleteCommand(plugin));
        handlers.put("edit",         new EditCommand(plugin));
        handlers.put("action",       new ActionCommand(plugin));
        handlers.put("pose",         new PoseCommand(plugin));
        handlers.put("resize",       new ResizeCommand(plugin));
        handlers.put("item",         new ItemCommand(plugin));
        handlers.put("equip",        new ItemCommand(plugin));
        handlers.put("viewdistance", new ViewDistanceCommand(plugin));
        handlers.put("skin",         new SkinCommand(plugin));
        handlers.put("setkey",       new SetKeyCommand(plugin));
        handlers.put("clone",        new CloneCommand(plugin));
        handlers.put("animate",      new AnimateCommand(plugin));
        handlers.put("animation_stop",new AnimateCommand(plugin));
        handlers.put("mob",          new MobCommand(plugin));
    }

    private boolean allowed(Player player, String subcommand) {
        if (player.hasPermission("CocoNPC.admin")) return true;
        String node = PERMISSIONS.get(subcommand);
        return node != null && player.hasPermission(node);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        if (!(commandSourceStack.getSender() instanceof Player player)) {
            Message.ONLY_PLAYERS.send(plugin, commandSourceStack.getSender());
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String subcommand = args[0].toLowerCase();
        if (!allowed(player, subcommand)) {
            Message.NO_PERMISSION.send(plugin, player);
            return;
        }

        SubCommand handler = handlers.get(subcommand);
        if (handler != null) {
            handler.execute(player, args);
        } else {
            sendHelp(player);
        }
    }

    private static net.kyori.adventure.text.Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    private void sendHelp(Player player) {
        Message.HELP_HEADER.send(plugin, player);
        Message.HELP_SPAWN.send(plugin, player);
        Message.HELP_LIST.send(plugin, player);
        Message.HELP_TELEPORT.send(plugin, player);
        Message.HELP_TELEPORT_HERE.send(plugin, player);
        Message.HELP_EDIT.send(plugin, player);
        Message.HELP_POSE.send(plugin, player);
        Message.HELP_ACTION.send(plugin, player);
        Message.HELP_RESIZE.send(plugin, player);
        Message.HELP_ITEM.send(plugin, player);
        Message.HELP_DELETE.send(plugin, player);
        Message.HELP_RELOAD.send(plugin, player);
        player.sendMessage(mm("<dark_gray> » <yellow>/coconpc clone <gray><source_id> <new_id> <dark_gray>- <gray>Clone an NPC"));
        player.sendMessage(mm("<dark_gray> » <yellow>/coconpc skin <gray><id> <skin> <dark_gray>- <gray>Change NPC skin"));
        player.sendMessage(mm("<dark_gray> » <yellow>/coconpc preload <dark_gray>- <gray>Preload local skins"));
        player.sendMessage(mm("<dark_gray> » <yellow>/coconpc setkey <gray><key> <dark_gray>- <gray>Set the MineSkin API key"));
        player.sendMessage(mm("<dark_gray> » <yellow>/coconpc mob <gray><list|spawn|remove|reload|info|animate|stopanim|tp|edit|action> <gray>[id] <dark_gray>- <gray>Manage .bbmodel mobs"));
    }

    private List<String> skinSuggestions(String prefix) {
        List<String> suggestions = new ArrayList<>();
        org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));

        java.io.File skinFolder = new java.io.File(plugin.getDataFolder(), "skins");
        java.io.File[] files = skinFolder.isDirectory() ? skinFolder.listFiles() : null;
        if (files != null) {
            for (java.io.File f : files) {
                if (f.getName().endsWith(".png")) {
                    suggestions.add(f.getName().substring(0, f.getName().length() - 4));
                }
            }
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> npcIdSuggestions(String prefix) {
        return plugin.getRegistry().getNpcs().values().stream()
                .map(npc -> npc.getId())
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
    }

    private List<String> mobIdSuggestions(String prefix) {
        return plugin.getMobRegistry().getDefinitions().stream()
                .map(com.pumpkings.coconpc.core.mob.MobDefinition::getId)
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> mobTargetSuggestions(String prefix) {
        List<String> options = new ArrayList<>();
        options.add("nearest");
        options.addAll(mobIdSuggestions(prefix));
        return filter(options, prefix);
    }

    private List<String> mobAnimationSuggestions(CommandSourceStack commandSourceStack,
                                                 String target, String prefix) {
        if (target == null || target.isBlank()) return List.of();

        com.pumpkings.coconpc.core.mob.MobRegistry registry = plugin.getMobRegistry();
        if (registry == null) return List.of();

        Collection<String> names = List.of();

        if (target.equalsIgnoreCase("nearest")) {
            if (commandSourceStack.getSender() instanceof Player player) {
                com.pumpkings.coconpc.core.mob.CustomMob mob = registry.getNearest(player.getLocation());
                if (mob != null) names = mob.getCustomAnimationNames();
            }
        } else if (target.startsWith("#")) {
            try {
                com.pumpkings.coconpc.core.mob.CustomMob mob = registry.getByInstance(
                        java.util.UUID.fromString(target.substring(1)));
                if (mob != null) names = mob.getCustomAnimationNames();
            } catch (IllegalArgumentException ignored) {
                // Invalid instance uuid: no suggestions.
            }
        } else {
            com.pumpkings.coconpc.core.mob.MobDefinition definition = registry.getDefinition(target);
            if (definition != null) {
                names = definition.getAnimationNames();
            }
        }

        return filter(new ArrayList<>(names), prefix);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length > 0 ? args[0].toLowerCase() : "";
            boolean isPlayer = commandSourceStack.getSender() instanceof Player;
            Player player = isPlayer ? (Player) commandSourceStack.getSender() : null;
            return handlers.keySet().stream()
                    .filter(s -> s.startsWith(prefix))
                    .filter(s -> player == null || allowed(player, s))
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "viewdistance" -> filter(List.of("16", "32", "48", "64", "80"), args[1]);
                case "mob" -> filter(List.of("list", "spawn", "remove", "reload", "info",
                        "animate", "stopanim", "tp", "edit", "action"), args[1]);
                case "create", "spawn" -> {
                    int nextId = 1;
                    while (getNpcById(String.valueOf(nextId)) != null) nextId++;
                    yield filter(List.of(String.valueOf(nextId)), args[1]);
                }
                default -> PERMISSIONS.containsKey(sub) ? npcIdSuggestions(args[1]) : List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "pose" -> Arrays.stream(NpcPose.values())
                        .map(NpcPose::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                case "animate" -> filter(new ArrayList<>(plugin.getAnimationManager().getAvailableAnimationNames()), args[2]);
                case "action" -> filter(List.of("list", "add", "remove", "clear"), args[2]);
                case "create", "spawn", "skin" -> skinSuggestions(args[2]);
                case "resize" -> filter(List.of("0.5", "1.0", "1.5", "2.0"), args[2]);
                case "item", "equip" -> filter(List.of("right", "left"), args[2]);
                case "mob" -> {
                    String mobAction = args[1].toLowerCase();
                    if (mobAction.equals("spawn") || mobAction.equals("info")) {
                        yield mobIdSuggestions(args[2]);
                    }
                    if (mobAction.equals("remove") || mobAction.equals("delete")) {
                        List<String> options = new ArrayList<>(mobIdSuggestions(args[2]));
                        options.add("all");
                        yield filter(options, args[2]);
                    }
                    if (mobAction.equals("animate") || mobAction.equals("stopanim")
                            || mobAction.equals("tp") || mobAction.equals("teleport")
                            || mobAction.equals("edit") || mobAction.equals("menu")
                            || mobAction.equals("action") || mobAction.equals("actions")) {
                        yield mobTargetSuggestions(args[2]);
                    }
                    yield List.of();
                }
                default -> List.of();
            };
        }

        if (args.length == 4) {
            return switch (sub) {
                case "item", "equip" -> filter(List.of("clear"), args[3]);
                case "action" -> suggestActionArg4(args);
                case "create", "spawn" -> filter(List.of("0.5", "1.0", "1.5", "2.0"), args[3]);
                case "mob" -> {
                    String mobAction = args[1].toLowerCase();
                    if (mobAction.equals("animate")) {
                        yield mobAnimationSuggestions(commandSourceStack, args[2], args[3]);
                    }
                    if (mobAction.equals("action") || mobAction.equals("actions")) {
                        yield filter(List.of("list", "add", "remove", "clear"), args[3]);
                    }
                    yield List.of();
                }
                default -> List.of();
            };
        }

        if (args.length == 5 && sub.equals("mob")) {
            String mobAction = args[1].toLowerCase();
            if (mobAction.equals("animate")) {
                return filter(List.of("loop", "true", "false"), args[4]);
            }
            if (mobAction.equals("action") || mobAction.equals("actions")) {
                String actionSub = args[3].toLowerCase();
                if (actionSub.equals("add")) {
                    return filter(ACTION_TYPES, args[4]);
                }
                if (Set.of("remove", "del", "delete").contains(actionSub)) {
                    return mobActionIndices(commandSourceStack, args[2], args[4]);
                }
            }
            return List.of();
        }

        if (args.length == 6 && sub.equals("mob")) {
            String mobAction = args[1].toLowerCase();
            if (mobAction.equals("action") || mobAction.equals("actions")) {
                if (args[3].equalsIgnoreCase("add")) {
                    if (normalizeActionType(args[4]).equals("animate")) {
                        return mobAnimationSuggestions(commandSourceStack, args[2], args[5]);
                    }
                    return suggestActionValueFor(args[4], args[5]);
                }
            }
            return List.of();
        }

        if (args.length == 7 && sub.equals("mob")) {
            String mobAction = args[1].toLowerCase();
            if (mobAction.equals("action") || mobAction.equals("actions")) {
                if (args[3].equalsIgnoreCase("add")) {
                    if (normalizeActionType(args[4]).equals("animate")) {
                        return filter(List.of("loop", "true", "false"), args[6]);
                    }
                    return suggestActionExtra(commandSourceStack, args[5], args[4], args[6]);
                }
            }
            return List.of();
        }

        if (args.length == 8 && sub.equals("mob")) {
            String mobAction = args[1].toLowerCase();
            if ((mobAction.equals("action") || mobAction.equals("actions"))
                    && args[3].equalsIgnoreCase("add")
                    && normalizeActionType(args[4]).equals("mob_animate")) {
                return filter(List.of("loop", "true", "false"), args[7]);
            }
            return List.of();
        }

        if (args.length == 5 && sub.equals("action") && args[2].equalsIgnoreCase("add")) {
            return suggestActionValue(args);
        }

        if (args.length == 6 && sub.equals("action") && args[2].equalsIgnoreCase("add")) {
            String type = args[3].toLowerCase();
            if (type.startsWith("[") && type.endsWith("]")) type = type.substring(1, type.length() - 1);
            if (type.equals("animate")) {
                List<String> options = new ArrayList<>(plugin.getAnimationManager().getAvailableAnimationNames());
                options.add("stop");
                return filter(options, args[5]);
            }
            if (type.equals("mob_animate")) {
                return mobAnimationSuggestions(commandSourceStack, args[4], args[5]);
            }
            if (type.equals("sound")) {
                return filter(List.of("1.0", "0.5", "2.0"), args[5]);
            }
            if (Set.of("give_item", "take_item").contains(type)) {
                return filter(List.of("1", "16", "32", "64"), args[5]);
            }
            if (type.equals("potion")) {
                return filter(List.of("200", "600", "1200"), args[5]);
            }
        }

        if (args.length == 7 && sub.equals("action") && args[2].equalsIgnoreCase("add")) {
            String type = args[3].toLowerCase();
            if (type.startsWith("[") && type.endsWith("]")) type = type.substring(1, type.length() - 1);
            if (type.equals("mob_animate")) {
                return filter(List.of("loop", "true", "false"), args[6]);
            }
            if (type.equals("sound")) {
                return filter(List.of("1.0", "0.8", "1.2", "1.5"), args[6]);
            }
            if (type.equals("potion")) {
                return filter(List.of("0", "1", "2"), args[6]);
            }
        }

        return List.of();
    }

    private List<String> suggestActionArg4(String[] args) {
        String actionSub = args[2].toLowerCase();
        if (actionSub.equals("add")) {
            return ACTION_TYPES.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (Set.of("remove", "del", "delete").contains(actionSub)) {
            NpcEntity npc = getNpcById(args[1]);
            if (npc != null) {
                List<String> actions = plugin.getConfigManager().getActions(npc);
                List<String> indices = new ArrayList<>();
                for (int i = 1; i <= actions.size(); i++) indices.add(String.valueOf(i));
                return indices.stream().filter(s -> s.startsWith(args[3])).collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private List<String> suggestActionValue(String[] args) {
        return suggestActionValueFor(args[3], args[4]);
    }

    private List<String> suggestActionValueFor(String rawType, String prefix) {
        String type = normalizeActionType(rawType);
        if (Set.of("message", "chat", "broadcast", "title", "subtitle", "player", "console").contains(type)) {
            return filter(List.of("<player>", "%player_name%"), prefix);
        }
        if (Set.of("give_exp", "take_exp", "require_money").contains(type)) {
            return filter(List.of("10", "100", "500", "1000"), prefix);
        }
        if (type.equals("cooldown")) {
            return filter(List.of("10", "30", "60", "300"), prefix);
        }
        if (type.equals("server")) {
            return filter(List.of("lobby", "survival"), prefix);
        }
        if (type.equals("animate")) {
            return npcIdSuggestions(prefix);
        }
        if (type.equals("mob_animate")) {
            return mobTargetSuggestions(prefix);
        }
        if (type.equals("sound")) {
            return Arrays.stream(org.bukkit.Sound.values())
                    .map(s -> s.name().toLowerCase())
                    .filter(s -> s.startsWith(prefix.toLowerCase()))
                    .limit(25)
                    .collect(Collectors.toList());
        }
        if (Set.of("give_item", "take_item").contains(type)) {
            return Arrays.stream(org.bukkit.Material.values())
                    .filter(org.bukkit.Material::isItem)
                    .map(m -> m.name().toLowerCase())
                    .filter(m -> m.startsWith(prefix.toLowerCase()))
                    .limit(25)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /** Second value of an action, e.g. animation name after {@code mob_animate <mob>}. */
    private List<String> suggestActionExtra(CommandSourceStack commandSourceStack, String target,
                                            String rawType, String prefix) {
        String type = normalizeActionType(rawType);
        if (type.equals("animate")) {
            List<String> options = new ArrayList<>(
                    plugin.getAnimationManager().getAvailableAnimationNames());
            options.add("stop");
            return filter(options, prefix);
        }
        if (type.equals("mob_animate")) {
            return mobAnimationSuggestions(commandSourceStack, target, prefix);
        }
        if (type.equals("sound")) {
            return filter(List.of("1.0", "0.8", "1.2", "1.5"), prefix);
        }
        if (type.equals("potion")) {
            return filter(List.of("0", "1", "2"), prefix);
        }
        return List.of();
    }

    private static String normalizeActionType(String rawType) {
        if (rawType == null) return "";
        String type = rawType.toLowerCase();
        if (type.startsWith("[") && type.endsWith("]")) {
            type = type.substring(1, type.length() - 1);
        }
        return type.equals("connect") ? "server" : type;
    }

    private List<String> mobActionIndices(CommandSourceStack commandSourceStack,
                                          String target, String prefix) {
        List<String> actions = mobActions(commandSourceStack, target);
        List<String> indices = new ArrayList<>();
        for (int i = 1; i <= actions.size(); i++) {
            indices.add(String.valueOf(i));
        }
        return filter(indices, prefix);
    }

    private List<String> mobActions(CommandSourceStack commandSourceStack, String target) {
        if (target == null || target.isBlank()) return List.of();

        com.pumpkings.coconpc.core.mob.MobRegistry registry = plugin.getMobRegistry();
        if (registry == null) return List.of();

        com.pumpkings.coconpc.core.mob.MobDefinition definition = null;
        if (target.equalsIgnoreCase("nearest")) {
            if (commandSourceStack.getSender() instanceof Player player) {
                com.pumpkings.coconpc.core.mob.CustomMob mob = registry.getNearest(player.getLocation());
                if (mob != null) definition = mob.getDefinition();
            }
        } else if (target.startsWith("#")) {
            try {
                com.pumpkings.coconpc.core.mob.CustomMob mob = registry.getByInstance(
                        java.util.UUID.fromString(target.substring(1)));
                if (mob != null) definition = mob.getDefinition();
            } catch (IllegalArgumentException ignored) {
                // Invalid instance uuid: no suggestions.
            }
        } else {
            definition = registry.getDefinition(target);
        }

        return definition != null ? definition.getActions() : List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private NpcEntity getNpcById(String id) {
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            if (id.equals(npc.getId())) return npc;
        }
        return null;
    }
}