package toast.specialMobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import net.minecraftforge.common.config.Configuration;

/**
 * This helper class automatically creates, stores, and retrieves properties.
 * Supported data types:
 * String, boolean, int, double
 *
 * Any property can be retrieved as an Object or String.
 * Any non-String property can also be retrieved as any other non-String property.
 * Retrieving a number as a boolean will produce a randomized output depending on the value.
 */
public abstract class Properties {
    /// Mapping of all properties in the mod to their values.
    private static final HashMap<String, Object> map = new HashMap<>();
    /// Common category names.
    public static final String ENCHANTS = "_enchants";
    public static final String SPAWNING = "_extra_spawning";
    public static final String GENERAL = "_general";
    public static final String STATS = "_mob_stats";
    public static final String LAVAMONSTER_GENERAL = "lavamonster_general";
    public static final String LAVAMONSTER_SPAWNING = "lavamonster_spawning";

    /// Property array, matched up to MONSTER_KEY[], vanilla monster species will only exist if the same index here is true.
    private static boolean[] monsterVanilla = new boolean[_SpecialMobs.MONSTER_KEY.length];
    /// Property array, matched up to MONSTER_KEY[], special monster species will only spawn if the same index here is true.
    private static boolean[] monsterSpawn = new boolean[_SpecialMobs.MONSTER_KEY.length];
    /// 2D property array, matched up to MONSTER_TYPES[][1], weights for each monster subspecies. monsterWeights[][0] refers to the vanilla weight.
    private static int[][] monsterWeights = new int[_SpecialMobs.MONSTER_KEY.length][];

    /// Set of dimension ids that special mobs should not spawn in.
    private static final HashSet<Integer> dimensionBlacklist = new HashSet<Integer>();

    public static Class<?> EntityFrostShardClass = null; // Thaumcraft frost focus
    public static Class<?> EntityIceArrow = null;        // Twilight forest ice arrow
    public static Class<?> ItemTFIceSword = null;        // Twilight Forest ice sword
    public static Class<?> ItemTFIceBomb = null;         // Twilight Forest ice bomb. 
    public static Class<?> ItemTFIronwoodSword = null;   // Twilight Forest ironwood sword
    public static Class<?> trash = null;
    
    static {
        for (int i = Properties.monsterWeights.length; i-- > 0;) {
            Properties.monsterWeights[i] = new int[_SpecialMobs.MONSTER_TYPES[i].length + 1];
        }
        // This unnatural act is to allow us to intercept various frost based damage sources
          try {
              trash = Class.forName((String)"toast.specialMobs.Properties$ThrowawayClass");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
          try {
              EntityFrostShardClass = Class.forName((String)"thaumcraft.common.entities.projectile.EntityFrostShard");
        } catch (ClassNotFoundException e) {
              EntityFrostShardClass = trash;
        }
          try {
              EntityIceArrow = Class.forName((String)"twilightforest.entity.EntityIceArrow");
        } catch (ClassNotFoundException e) {
              EntityIceArrow = trash;
        }
          try {
              ItemTFIceSword = Class.forName((String)"twilightforest.item.ItemTFIceSword");
        } catch (ClassNotFoundException e) {
              ItemTFIceSword = trash;
        }
          try {
              ItemTFIceBomb = Class.forName((String)"twilightforest.item.ItemTFIceBomb");
        } catch (ClassNotFoundException e) {
              ItemTFIceBomb = trash;
        }
          try {
              ItemTFIronwoodSword = Class.forName((String)"twilightforest.item.ItemTFIronwoodSword");
        } catch (ClassNotFoundException e) {
              ItemTFIronwoodSword = trash;
        }
    }
    
    public static class ThrowawayClass {
        // This is a throwaway class to use when the Class.forName() functions fail.
    }

    /// Initializes these properties.
    public static void init(Configuration config) {
        int mobsAdded = _SpecialMobs.MONSTER_KEY.length;
        for (int i = 0; i < _SpecialMobs.MONSTER_KEY.length; i++) {
            mobsAdded += _SpecialMobs.MONSTER_TYPES[i].length;
        }

        config.load();

        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_animated_texture", true, "If true, lava monsters will have animated textures.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_armor", 0, "The amount of armor lava monsters have.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_health", 24.0, "Lava monsters' maximum health.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_cooldown", 60, "Ticks a monster must wait after attacking before it can start winding up again.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_shots", 3, "Number of fireballs shot with each attack.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_spacing", 6, "Ticks between each fireball shot in an attack.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_attack_windup", 60, "Ticks it takes before a monster can start an attack.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_fires", false, "Lava monsters on land cause fires. Enabling this makes it hard to get loot.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_basic_loot", false, "Lava monsters will only drop basic loot: coal and fire charges.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_time", 60, "Average ticks between heal attempts when the lava monster is standing in lava.");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_percent", .25, "Percent to heal when standing in lava");
        Properties.add(config, Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_max", 12, "Maximum half-hearts to heal when standing in lava. Useful to keep infernals reasonable");

        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_Nether_spawn", true, "If true, lava monsters will spawn in the Nether.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_depth_hazard", false, "If true, lava monsters will not spawn above layer 16.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_flowing_lava", false, "If true, lava monsters do not require a source block to spawn.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_shallow_lava", false, "If true, lava monsters will be able to spawn in lava one block deep.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_chance", 0.5, "The chance for a lava monster spawn attempt to be tried in a chunk. Might not find lava anyways.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_frequency", 79, "The number of ticks between each lava monster spawn attempt.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_max", 4, "The maximum number of lava monsters in the `area` in range of a player. Some could be further away.");
        Properties.add(config, Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_range", 5, "The radius of the `area` in chunks to spawn around the player. The area monsters can actually spawn is 1 less.");

        config.addCustomCategoryComment(Properties.LAVAMONSTER_GENERAL, "General and/or miscellaneous options for Lava Monsters.");
        config.addCustomCategoryComment(Properties.LAVAMONSTER_SPAWNING, "Options dictating the spawning algorithm for Lava Monsters.");
        
        Properties.add(config, Properties.ENCHANTS, "pain_bow", 160);
        Properties.add(config, Properties.ENCHANTS, "pain_sword", 161);
        Properties.add(config, Properties.ENCHANTS, "pain_damage",  1.34, "The amount of direct damage done by each level of the Pain enchant");
        Properties.add(config, Properties.ENCHANTS, "plague_bow", 162);
        Properties.add(config, Properties.ENCHANTS, "plague_sword", 163);
        Properties.add(config, Properties.ENCHANTS, "poison_bow", 164);
        Properties.add(config, Properties.ENCHANTS, "poison_sword", 165);
        Properties.add(config, Properties.ENCHANTS, "poison_lvl", 2, "The level of poison inflicted by Poison enchant (used by poison skeletons) (min 1).");
        Properties.add(config, Properties.ENCHANTS, "poison_stacks", true, "If true, poison inflicted by poison enchantment stacks");


        Properties.add(config, Properties.SPAWNING, "end_ender_creeper", 1, 0, Integer.MAX_VALUE);
        Properties.add(config, Properties.SPAWNING, "nether_fire_creeper", 10, 0, Integer.MAX_VALUE);
        Properties.add(config, Properties.SPAWNING, "overworld_ghast_mount", 1, 0, Integer.MAX_VALUE);

        Properties.loadIntSet(config, Properties.GENERAL, "dimension_blacklist", "", "Comma-separated list of each dimension to prevent this mod from replacing mobs in. Default is none.", Properties.dimensionBlacklist);
        
        Properties.add(config, Properties.GENERAL, "random_scaling", 0.3, "The maximum magnitude for random size scaling (scaling * 50% = max difference %). Setting this to 0 disables random size scaling. Default is +/-15%");
        Properties.add(config, Properties.GENERAL, "spawn_eggs", false, "If true, the game will attempt to make a spawn egg for each special mob. Be warned, this will eat up " + mobsAdded + " global entity ids. Default is false.");
        Properties.add(config, Properties.GENERAL, "chat_enabled", true, "Enable snark chat from mobs");
        Properties.add(config, Properties.GENERAL, "chat_range", 10, 2, 30, "Sets the chat range for snarky/super messages");
        
        Properties.add(config, Properties.STATS, "baby_skeleton_chance", 0.05, 0.0, 1.0, "(0 <= x <= 1) Chance that a skeleton will spawn as a baby. Default is 5%.");
        Properties.add(config, Properties.STATS, "blaze_snowball_hits",  4, 2, 10, "How many snowballs it takes to kill a blaze(does not account for cooldown or regen)");
        Properties.add(config, Properties.STATS, "bow_chance_pigzombie", 0.25, 0.0, 1.0, "(0 <= x <= 1) Chance that a zombie pigman will spawn with a bow, if possible. Default is 25%.");
        Properties.add(config, Properties.STATS, "bow_chance_skeleton", 0.95, 0.0, 1.0, "(0 <= x <= 1) Chance that a skeleton will spawn with a bow, if possible. Default is 95%.");
        Properties.add(config, Properties.STATS, "bow_chance_wither", 0.05, 0.0, 1.0, "(0 <= x <= 1) Chance that a wither skeleton will spawn with a bow, if possible. Default is 5%.");
        Properties.add(config, Properties.STATS, "bow_chance_zombie", 0.05, 0.0, 1.0, "(0 <= x <= 1) Chance that a zombie will spawn with a bow, if possible. Default is 5%.");
        Properties.add(config, Properties.STATS, "conflagration_snowball_hits",  2, 2, 10, "How many snowballs it takes to kill a conflagration(does not account for cooldown or regen)");
        Properties.add(config, Properties.STATS, "creeper_charge_chance", 0.01, 0.0, 1.0, "(0 <= x <= 1) Chance that any creeper spawned during a thunderstorm will be charged. Default is 1%.");
        Properties.add(config, Properties.STATS, "enderman_griefing", true, "(True/false) If true, endermen will pick up blocks and place them around randomly, as in vanilla. Default is true.");
        Properties.add(config, Properties.STATS, "hostile_cavespiders", 1.0, 0.0, 1.0, "(0 <= x <= 1) Chance that a cave spider will spawn aggressive in daylight. Default is 100%.");
        Properties.add(config, Properties.STATS, "hostile_pigzombies", 0.05, 0.0, 1.0, "(0 <= x <= 1) Chance that a zombie pigman will spawn already mad. Default is 5%.");
        Properties.add(config, Properties.STATS, "hostile_silverfish", 0.2, 0.0, 1.0, "(0 <= x <= 1) Chance that a silverfish will spawn already calling for reinforcements. Default is 20%.");
        Properties.add(config, Properties.STATS, "hostile_spiders", 0.1, 0.0, 1.0, "(0 <= x <= 1) Chance that a spider will spawn aggressive in daylight. Default is 10%.");
        Properties.add(config, Properties.STATS, "spit_chance_cavespider", 0.1, 0.0, 1.0, "(0 <= x <= 1) Chance that a cave spider will spawn with a spitting attack. Default is 10%.");
        Properties.add(config, Properties.STATS, "spit_chance_spider", 0.05, 0.0, 1.0, "(0 <= x <= 1) Chance that a spider will spawn with a spitting attack. Default is 5%.");
        Properties.add(config, Properties.STATS, "tiny_slime_damage", true, "If true, tiny special slimes (including vanilla replacements) will be able to deal damage to players. Default is true.");
        Properties.add(config, Properties.STATS, "villager_infection", 1.0, 0.0, 1.0, "(0 <= x <= 1) Chance that a villager will be infected when killed by a zombie. Default is 100%.");
        Properties.add(config, Properties.STATS, "xray_ghosts", false, "(True/false) If false, ghost spiders and faint ghasts will require line of sight to aggro, unlike their normal counterparts. Default is false.");

        Properties.initializeArrays(config, "monster", _SpecialMobs.MONSTER_KEY, _SpecialMobs.MONSTER_TYPES, Properties.monsterVanilla, Properties.monsterSpawn, Properties.monsterWeights);

        config.addCustomCategoryComment(Properties.ENCHANTS, "Info for all enchantments added by this mod. Set the id to -1 to disable any specific enchantment.");
        config.addCustomCategoryComment(Properties.SPAWNING, "Weighted chances for each additional spawn. Set the weight to 0 to disable the spawn.");
        config.addCustomCategoryComment(Properties.GENERAL, "Spawn rates for each mob type and miscellaneous options.");
        config.addCustomCategoryComment(Properties.STATS, "Additional options for mobs\' stats, such as the chance for the mob to have a bow or to be unusually hostile.");

        config.save();
    }

    /// Initializes an integer hash set property.
    private static void loadIntSet(Configuration config, String category, String field, String defaultValue, String comment, HashSet<Integer> intSet) {
        intSet.clear();
        String[] intArray = { config.get(category, field, defaultValue, comment).getString() };
        if (!intArray[0].trim().isEmpty()) {
            intArray = intArray[0].split(",");
            for (int i = 0; i < intArray.length; i++) {
                try {
                    intSet.add(Integer.parseInt(intArray[i].trim()));
                }
                catch (Exception ex) {
                    throw new RuntimeException("Invalid dimension blacklist entry: " + intArray[i].trim(), ex);
                }
            }
        }
    }

    /// Initializes specific monster properties.
    private static void initializeArrays(Configuration config, String category, String[] key, String[][] types, boolean[] keyVanilla, boolean[] keyValues, int[][] typeWeights) {
        String pluralKey;
        String MOB_CATEGORY;
        for (int i = 0; i < key.length; i++) {
            pluralKey = key[i].toLowerCase();
            MOB_CATEGORY = pluralKey + "_rates";
            if (key[i] == "Enderman") {
                pluralKey = "endermen";
            }
            else if (key[i] == "Witch") {
                pluralKey = "witches";
            }
            else {
                pluralKey = pluralKey + "s";
            }

            keyVanilla[i] = config.get(MOB_CATEGORY, "_allow_vanilla", false, "If this false, vanilla " + pluralKey + " will be replaced with similar Special Mobs versions if not \"special\".").getBoolean(true);
            keyValues[i] = config.get(MOB_CATEGORY, "_special_" + pluralKey, true, "Set this to false to disable all special " + pluralKey + ".").getBoolean(true);
            typeWeights[i][0] = Math.max(0, config.get(MOB_CATEGORY, "_vanilla", 30).getInt(30));

            for (int j = 0; j < types[i].length; j++) {
                typeWeights[i][j + 1] = Math.max(0, config.get(MOB_CATEGORY, types[i][j].toLowerCase(), 1).getInt(1));
            }

            config.addCustomCategoryComment(MOB_CATEGORY, "Weighted chances for each mob subtype when a vanilla " + _SpecialMobs.localizeName(key[i]) + " is spawned.");
        }
    }

    /// Gets the mod's random number generator.
    public static Random random() {
        return _SpecialMobs.random;
    }

    /// Passes to the mod.
    public static void debugException(String message) {
        _SpecialMobs.debugException(message);
    }

    /// Returns values for private variables.
    public static boolean[] monsterVanilla() {
        return Properties.monsterVanilla;
    }
    public static boolean[] monsterSpawn() {
        return Properties.monsterSpawn;
    }
    public static int[][] monsterWeights() {
        return Properties.monsterWeights;
    }
    public static HashSet<Integer> dimensionBlacklist() {
        return Properties.dimensionBlacklist;
    }

    /// Loads the property as the specified value.
    public static void add(Configuration config, String category, String field, String defaultValue, String comment) {
        Properties.map.put(category + "@" + field, config.get(category, field, defaultValue, comment).getString());
    }

    public static void add(Configuration config, String category, String field, int defaultValue) {
        Properties.map.put(category + "@" + field, Integer.valueOf(config.get(category, field, defaultValue).getInt(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, int defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Integer.valueOf(config.get(category, field, defaultValue, comment).getInt(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, int defaultValue, int minValue, int maxValue) {
        Properties.map.put(category + "@" + field, Integer.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue).getInt(defaultValue)))));
    }

    public static void add(Configuration config, String category, String field, int defaultValue, int minValue, int maxValue, String comment) {
        Properties.map.put(category + "@" + field, Integer.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue, comment).getInt(defaultValue)))));
    }

    public static void add(Configuration config, String category, String field, boolean defaultValue) {
        Properties.map.put(category + "@" + field, Boolean.valueOf(config.get(category, field, defaultValue).getBoolean(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, boolean defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Boolean.valueOf(config.get(category, field, defaultValue, comment).getBoolean(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, double defaultValue) {
        Properties.map.put(category + "@" + field, Double.valueOf(config.get(category, field, defaultValue).getDouble(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, double defaultValue, String comment) {
        Properties.map.put(category + "@" + field, Double.valueOf(config.get(category, field, defaultValue, comment).getDouble(defaultValue)));
    }

    public static void add(Configuration config, String category, String field, double defaultValue, double minValue, double maxValue, String comment) {
        Properties.map.put(category + "@" + field, Double.valueOf(Math.max(minValue, Math.min(maxValue, config.get(category, field, defaultValue, comment).getDouble(defaultValue)))));
    }

    /// Gets the Object property.
    public static Object getProperty(String category, String field) {
        return Properties.map.get(category + "@" + field);
    }

    /// Gets the value of the property (instead of an Object representing it).
    public static String getString(String category, String field) {
        return Properties.getProperty(category, field).toString();
    }

    public static boolean getBoolean(String category, String field) {
        return Properties.getBoolean(category, field, Properties.random());
    }

    public static boolean getBoolean(String category, String field, Random random) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue();
        if (property instanceof Integer)
            return random.nextInt( ((Number) property).intValue()) == 0;
        if (property instanceof Double)
            return random.nextDouble() < ((Number) property).doubleValue();
        Properties.debugException("Tried to get boolean for invalid property! @" + property.getClass().getName());
        return false;
    }

    public static int getInt(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number) property).intValue();
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue() ? 1 : 0;
        Properties.debugException("Tried to get int for invalid property! @" + property == null ? "(null)" : property.getClass().getName());
        return 0;
    }

    public static double getDouble(String category, String field) {
        Object property = Properties.getProperty(category, field);
        if (property instanceof Number)
            return ((Number) property).doubleValue();
        if (property instanceof Boolean)
            return ((Boolean) property).booleanValue() ? 1.0 : 0.0;
        Properties.debugException("Tried to get double for invalid property! @" + property.getClass().getName());
        return 0.0;
    }
}