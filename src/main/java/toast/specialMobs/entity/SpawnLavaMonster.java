package toast.specialMobs.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import toast.specialMobs.Properties;
import toast.specialMobs._SpecialMobs;

public class SpawnLavaMonster {
    /// Handy properties for this class.
    public static final int     SPAWN_FREQUENCY = Properties.getInt(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_frequency");
    public static final int     SPAWN_MAX =       Properties.getInt(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_max");
    public static final int     SPAWN_RANGE =     Properties.getInt(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_range");
    public static final double  SPAWN_CHANCE =    Properties.getDouble(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_chance");

    public static final boolean DEPTH_HAZARD = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_depth_hazard");
    public static final boolean SHALLOW_LAVA = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_shallow_lava");
    public static final boolean FLOWING_LAVA = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_flowing_lava");
    public static final boolean NETHER_SPAWN = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_Nether_spawn");

    /// Counter to the next spawn attempt.
    private int spawnTime = 0;
    /// A map of eligible spawning chunks.
    private HashMap<ChunkCoordIntPair, Boolean> eligibleChunksForSpawning = new HashMap<ChunkCoordIntPair, Boolean>(); // location of chunk to spawn in, is the chunk far from the player

    public SpawnLavaMonster() {
        FMLCommonHandler.instance().bus().register(this);
    }

    /// Returns true if a lava monster can spawn at the given location.
    public static boolean canLavaMonsterSpawnAtLocation(World world, int x, int y, int z) {
        return (!SpawnLavaMonster.DEPTH_HAZARD || y <= 16 || world.provider.isHellWorld) && world.getBlock(x, y, z).getMaterial() == Material.lava && (world.getBlock(x, y + 1, z).getMaterial() == Material.lava || SpawnLavaMonster.SHALLOW_LAVA && !world.isBlockNormalCubeDefault(x, y + 1, z, true)) && (SpawnLavaMonster.FLOWING_LAVA || world.getBlockMetadata(x, y, z) == 0 || world.getBlockMetadata(x, y + 1, z) == 0);
    }

    /// Spawns lava monsters in the world. Returns the number spawned for debugging purposes.
    @SuppressWarnings("unchecked")
    private int performSpawning(WorldServer world) {
        if( world.playerEntities.size() == 0) { // No player entities in this world, don't spawn.
            return 0;
        }
        if (!SpawnLavaMonster.NETHER_SPAWN && world.provider.isHellWorld || (++this.spawnTime < SpawnLavaMonster.SPAWN_FREQUENCY))
            return 0;

        // Reset spawnTimer to 0
        this.spawnTime = 0;
        this.eligibleChunksForSpawning.clear();
        for (EntityPlayer player : (List<EntityPlayer>) world.playerEntities) { // This cycles across *all* player entities
            int chunkX = MathHelper.floor_double(player.posX / 16.0);
            int chunkZ = MathHelper.floor_double(player.posZ / 16.0);
            byte spawnRange = (byte) (SPAWN_RANGE&0xff); /// In chunks.
            for (int x = -spawnRange; x <= spawnRange; x++) {
                for (int z = -spawnRange; z <= spawnRange; z++) {
                    // Best guess, this stuff is trying to mark chunks on the edge of the spawn range.
                    // And if a chunk is already marked, it then clears it.  This prevents spawns far away from players that won't get seen etc anyways.
                    boolean isEdge = x == -spawnRange || x == spawnRange || z == -spawnRange || z == spawnRange;
                    ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(x + chunkX, z + chunkZ);
                    if (!isEdge) {
                        this.eligibleChunksForSpawning.put(chunkCoord, Boolean.valueOf(false)); // If a chunk is already in the list, override it as false, it's not an edge chunk any more
                    }
                    else if (!this.eligibleChunksForSpawning.containsKey(chunkCoord)) { // isEdge is true, and the chunk is not already in the HashMap
                        this.eligibleChunksForSpawning.put(chunkCoord, Boolean.valueOf(true)); // OK to place a lava monster in this spot
                    }
                }
            }
        }

        int numberSpawned = 0;
        ChunkCoordinates spawnCoords = world.getSpawnPoint();
        int adjustTotalChunks=(SPAWN_RANGE*2+1)*(SPAWN_RANGE*2+1); // Above for loop is over *all* player entities. To get the correct number of maximum lava monsters, we have to adjust
                                                                   // by how many lava monsters are allowed across all possible spawning chunks. SPAWN_MAX per player
        // Since it only checks here for SPAWN_MAX, it's possible there could be more than SPAWN_MAX lava monsters in the world.
        int countInWorld = world.countEntities(EntityLavaMonster.class);
        int maxAllowedInWorld = SpawnLavaMonster.SPAWN_MAX * this.eligibleChunksForSpawning.size() / adjustTotalChunks;
        _SpecialMobs.debugConsole("Lava monster count " + countInWorld + "  Max allowed " + maxAllowedInWorld);
        if ( countInWorld < maxAllowedInWorld ) {
            ArrayList<ChunkCoordIntPair> chunks = new ArrayList<>(this.eligibleChunksForSpawning.keySet());
            Collections.shuffle(chunks);
            chunkIterator: for (ChunkCoordIntPair chunkCoord : chunks) {
                if (numberSpawned >= (maxAllowedInWorld - countInWorld)) {
                    break;
                }
                if (!this.eligibleChunksForSpawning.get(chunkCoord).booleanValue()) { // Don't spawn lava monsters on the chunks furthest from the player. Keeps them a little close and keeps a buffer distance
                    ChunkPosition chunkPos = this.getRandomSpawningPointInChunk(world, chunkCoord.chunkXPos, chunkCoord.chunkZPos);
                    int x = chunkPos.chunkPosX;
                    int y = chunkPos.chunkPosY;
                    int z = chunkPos.chunkPosZ;
                    // Decide to skip spawning something in this chunk
                    if ((SPAWN_CHANCE*100) < world.rand.nextInt(100)) {
                        // _SpecialMobs.debugConsole("Skipping chunk X " + x + "  Z " + z);
                        continue;
                    }
                    if (world.isBlockNormalCubeDefault(x, y, z, true)) {
                        continue;
                    }
                    byte groupRadius = 6;
                    for (int groupSpawnAttempt = 3; groupSpawnAttempt-- > 0;) {
                        int X = x;
                        int Y = y;
                        int Z = z;
                        for (int spawnAttempt = 4; spawnAttempt-- > 0;) {
                            X += world.rand.nextInt(groupRadius) - world.rand.nextInt(groupRadius);
                            Y += world.rand.nextInt(1) - world.rand.nextInt(1);
                            Z += world.rand.nextInt(groupRadius) - world.rand.nextInt(groupRadius);
                            if (SpawnLavaMonster.canLavaMonsterSpawnAtLocation(world, X, Y, Z)) {
                                float posX = X + 0.5F;
                                float posY = Y;
                                float posZ = Z + 0.5F;
                                if (world.getClosestPlayer(posX, posY, posZ, 24.0) == null) { // No player closer than 24 blocks
                                    float spawnX = posX - spawnCoords.posX;
                                    float spawnY = posY - spawnCoords.posY;
                                    float spawnZ = posZ - spawnCoords.posZ;
                                    float spawnDist = spawnX * spawnX + spawnY * spawnY + spawnZ * spawnZ;
                                    if (spawnDist >= 576.0F) {  // Make sure we aren't filling the spawn chunks with lava monsters
                                        EntityLavaMonster lavaMonster = new EntityLavaMonster(world);
                                        lavaMonster.setLocationAndAngles(posX, posY, posZ, world.rand.nextFloat() * 360.0F, 0.0F);
                                        Result canSpawn = ForgeEventFactory.canEntitySpawn(lavaMonster, world, posX, posY, posZ);
                                        if (canSpawn == Result.ALLOW || canSpawn == Result.DEFAULT && lavaMonster.getCanSpawnHere()) {
                                            numberSpawned++;
                                            _SpecialMobs.debugConsole("Mob Spawned at X " + posX + "  Y " + posY + "  Z " + posZ);
                                            world.spawnEntityInWorld(lavaMonster);
                                            if (!ForgeEventFactory.doSpecialSpawn(lavaMonster, world, posX, posY, posZ)) {
                                                lavaMonster.onSpawnWithEgg((IEntityLivingData) null);
                                            }
                                            continue chunkIterator;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return numberSpawned;
    }

    /// Returns a randomized chunk position within the given chunk.
    private ChunkPosition getRandomSpawningPointInChunk(World world, int chunkX, int chunkZ) {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        int x = (chunkX << 4) + world.rand.nextInt(16);
        int z = (chunkZ << 4) + world.rand.nextInt(16);
        int y = world.rand.nextInt(chunk == null ? world.getActualHeight() : chunk.getTopFilledSegment() + 16 - 1);
        return new ChunkPosition(x, y, z);
    }

    /**
     * Called each tick.
     * TickEvent.Type type = the type of tick.
     * Side side = the side this tick is on.
     * TickEvent.Phase phase = the phase of this tick (START, END).
     * 
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (event.world instanceof WorldServer) {
                this.performSpawning((WorldServer) event.world);
            }
        }
    }
}