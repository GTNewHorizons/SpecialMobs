package toast.specialMobs;

import java.util.ArrayDeque;

import net.minecraft.entity.EntityLiving;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class TickHandler {

    // Stack of entities that need to be spawned.
    public static ArrayDeque<ReplacementEntry> entityStack = new ArrayDeque<>();

    private long lastStackTrimmedMessageTime = 0;

    public TickHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Called each tick. TickEvent.Type type = the type of tick. Side side = the side this tick is on. TickEvent.Phase
     * phase = the phase of this tick (START, END).
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (!TickHandler.entityStack.isEmpty()) {
                ReplacementEntry entry;
                for (int limit = 10; limit-- > 0;) {
                    entry = TickHandler.entityStack.pollFirst();
                    if (entry == null) {
                        break;
                    }
                    entry.replace();
                }
                // If processing the entity queue would take longer than a minute, drop the list to avoid running out of
                // memory
                if (TickHandler.entityStack.size() > 10 * 20 * 60) {
                    long now = System.currentTimeMillis();
                    if (Math.abs(now - lastStackTrimmedMessageTime) < 10_000) {
                        _SpecialMobs.console(
                                "Entity transformation list reached a size of " + TickHandler.entityStack.size()
                                        + ", force-clearing.");
                        lastStackTrimmedMessageTime = now;
                    }
                    TickHandler.entityStack.clear();
                }
            }
        }
    }

    // Puts the mob into the stack.
    public static void markEntityToBeReplaced(EntityLiving entity) {
        TickHandler.entityStack.add(new ReplacementEntry(entity));
    }
}
