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

public class SpawnLavaMonster {
    /// Handy properties for this class.
    public static final int SPAWN_FREQUENCY = Properties.getInt(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_frequency");
    public static final boolean DEPTH_HAZARD = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_depth_hazard");
    public static final boolean SHALLOW_LAVA = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_shallow_lava");
    public static final boolean FLOWING_LAVA = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_flowing_lava");
    public static final boolean NETHER_SPAWN = Properties.getBoolean(Properties.LAVAMONSTER_SPAWNING, "lavamonster_Nether_spawn");
    /// The max number of lava monsters around.
    private static final int maxNumberOfCreature = 20;

    /// Counter to the next spawn attempt.
    private int spawnTime = 0;
    /// A map of eligible spawning chunks.
    private HashMap<ChunkCoordIntPair, Boolean> eligibleChunksForSpawning = new HashMap<ChunkCoordIntPair, Boolean>();

    public SpawnLavaMonster() {
        FMLCommonHandler.instance().bus().register(this);
    }

    /// Returns true if a lava monster can spawn at the given location.
    public static boolean canLavaMonsterSpawnAtLocation(World world, int x, int y, int z) {
        return (!SpawnLavaMonster.DEPTH_HAZARD || y <= 16 || world.provider.isHellWorld) && world.getBlock(x, y, z).getMaterial() == Material.lava && (world.getBlock(x, y + 1, z).getMaterial() == Material.lava || SpawnLavaMonster.SHALLOW_LAVA && !world.isBlockNormalCubeDefault(x, y + 1, z, true)) && (SpawnLavaMonster.FLOWING_LAVA || world.getBlockMetadata(x, y, z) == 0 || world.getBlockMetadata(x, y + 1, z) == 0);
    }

    /// Spawns lava monsters in the world. Returns the number spawned for debugging purposes.
    private int performSpawning(WorldServer world) {
        if (!SpawnLavaMonster.NETHER_SPAWN && world.provider.isHellWorld || ++this.spawnTime < SpawnLavaMonster.SPAWN_FREQUENCY)
            return 0;

        this.eligibleChunksForSpawning.clear();
        for (EntityPlayer player : (List<EntityPlayer>) world.playerEntities) {
            int chunkX = MathHelper.floor_double(player.posX / 16.0);
            int chunkZ = MathHelper.floor_double(player.posZ / 16.0);
            byte spawnRange = 8; /// In chunks.
            for (int x = -spawnRange; x <= spawnRange; x++) {
                for (int z = -spawnRange; z <= spawnRange; z++) {
                    boolean isEdge = x == -spawnRange || x == spawnRange || z == -spawnRange || z == spawnRange;
                    ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(x + chunkX, z + chunkZ);
                    if (!isEdge) {
                        this.eligibleChunksForSpawning.put(chunkCoord, Boolean.valueOf(false));
                    }
                    else if (!this.eligibleChunksForSpawning.containsKey(chunkCoord)) {
                        this.eligibleChunksForSpawning.put(chunkCoord, Boolean.valueOf(true));
                    }
                }
            }
        }

        int numberSpawned = 0;
        ChunkCoordinates spawnCoords = world.getSpawnPoint();
        if (world.countEntities(EntityLavaMonster.class) <= SpawnLavaMonster.maxNumberOfCreature * this.eligibleChunksForSpawning.size() / 256) {
            ArrayList<ChunkCoordIntPair> chunks = new ArrayList(this.eligibleChunksForSpawning.keySet());
            Collections.shuffle(chunks);
            chunkIterator: for (ChunkCoordIntPair chunkCoord : chunks) {
                if (!this.eligibleChunksForSpawning.get(chunkCoord).booleanValue()) {
                    ChunkPosition chunkPos = this.getRandomSpawningPointInChunk(world, chunkCoord.chunkXPos, chunkCoord.chunkZPos);
                    int x = chunkPos.chunkPosX;
                    int y = chunkPos.chunkPosY;
                    int z = chunkPos.chunkPosZ;
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
                                if (world.getClosestPlayer(posX, posY, posZ, 24.0) == null) {
                                    float spawnX = posX - spawnCoords.posX;
                                    float spawnY = posY - spawnCoords.posY;
                                    float spawnZ = posZ - spawnCoords.posZ;
                                    float spawnDist = spawnX * spawnX + spawnY * spawnY + spawnZ * spawnZ;
                                    if (spawnDist >= 576.0F) {
                                        EntityLavaMonster lavaMonster = new EntityLavaMonster(world);
                                        lavaMonster.setLocationAndAngles(posX, posY, posZ, world.rand.nextFloat() * 360.0F, 0.0F);
                                        Result canSpawn = ForgeEventFactory.canEntitySpawn(lavaMonster, world, posX, posY, posZ);
                                        if (canSpawn == Result.ALLOW || canSpawn == Result.DEFAULT && lavaMonster.getCanSpawnHere()) {
                                            numberSpawned++;
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