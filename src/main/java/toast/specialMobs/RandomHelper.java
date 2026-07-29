package toast.specialMobs;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public abstract class RandomHelper {

    // The total weights for each MONSTER_KEY.
    public static final int[] totalMonsterWeights = RandomHelper.buildTotalMonsterWeights();
    // The registered entity name of each species, matched up to Properties.monsterWeights()[key]. These names are
    // constant, so building them once keeps every single mob replacement from concatenating the same pool of strings.
    private static final String[][] monsterEntityNames = RandomHelper.buildMonsterEntityNames();

    // Produces a random monster based on the monster key given.
    public static EntityLiving nextMonster(int key, World world) {
        return RandomHelper.nextEntity(
                key,
                world,
                "monster",
                RandomHelper.monsterEntityNames[key],
                Properties.monsterWeights()[key],
                RandomHelper.totalMonsterWeights[key]);
    }

    // Produces a random mob based on the info given, or null if the mob should not be replaced.
    public static EntityLiving nextEntity(int key, World world, String category, String[] entityNames, int[] weights,
            int totalWeight) {
        int choice = _SpecialMobs.random.nextInt(totalWeight);
        for (int i = weights.length; i-- > 0;) {
            choice -= weights[i];
            if (choice < 0) {
                // Index 0 is the vanilla replacement, the rest line up with the sub-species.
                if (i == 0 && Properties.monsterVanilla()[key]) return null;
                return (EntityLiving) EntityList.createEntityByName(entityNames[i], world);
            }
        }
        _SpecialMobs.debugException("Weighting error: " + category + " (" + key + ")!");
        return null;
    }

    // Builds the monsterEntityNames[][] variable automatically, mirroring how _SpecialMobs registers each species.
    private static String[][] buildMonsterEntityNames() {
        String[][] names = new String[_SpecialMobs.MONSTER_KEY.length][];
        for (int i = names.length; i-- > 0;) {
            String monsterKey = _SpecialMobs.MONSTER_KEY[i];
            String[] types = _SpecialMobs.MONSTER_TYPES[i];
            names[i] = new String[types.length + 1];
            names[i][0] = _SpecialMobs.MODID + ".Special" + monsterKey;
            for (int j = types.length; j-- > 0;) {
                names[i][j + 1] = _SpecialMobs.MODID + "." + types[j] + monsterKey;
            }
        }
        return names;
    }

    // Builds the totalMonsterWeights[] variable automatically.
    private static int[] buildTotalMonsterWeights() {
        int[] totalWeights = new int[_SpecialMobs.MONSTER_KEY.length];
        int[][] weights = Properties.monsterWeights();
        for (int i = weights.length; i-- > 0;) {
            for (int j = weights[i].length; j-- > 0;) {
                totalWeights[i] += weights[i][j];
            }
        }
        return totalWeights;
    }
}
