package toast.specialMobs.waila;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaEntityAccessor;
import mcp.mobius.waila.api.IWailaEntityProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import toast.specialMobs.entity.ISpecialMob;

public class WailaCompat implements IWailaEntityProvider {

    private static final WailaCompat INSTANCE = new WailaCompat();

    public static void load(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(INSTANCE, ISpecialMob.class);
    }

    @Override
    public Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config) {
        return null; // not used
    }

    @Override
    public List<String> getWailaHead(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
            IWailaConfigHandler config) {
        return null; // not used
    }

    @Override
    public List<String> getWailaBody(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
            IWailaConfigHandler config) {
        String name = EntityList.getEntityString(entity);
        String locKey = "entity." + name + ".desc.";
        String loc = null;
        for (int line = 1; !(loc = StatCollector.translateToLocal(locKey + line)).equals(locKey + line); line++) {
            currenttip.add(loc);
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaTail(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
            IWailaConfigHandler config) {
        return null; // not used
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, Entity ent, NBTTagCompound tag, World world) {
        return null;
    }

}
