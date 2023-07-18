package toast.specialMobs.entity;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.FakePlayer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import toast.specialMobs.Properties;

/**
 * This interface is implemented by all monsters in this mod. It allows for commonly used data to be stored conveniently
 * and for attribute changes to be appropriately applied.
 */
public interface ISpecialMob extends IMob {

    public static final boolean CHAT_ENABLED = Properties.getBoolean(Properties.GENERAL, "chat_enabled");
    public static final int CHAT_RANGE = Properties.getInt(Properties.GENERAL, "chat_range");

    /**
     * @return this mob's special data
     */
    public SpecialMobData getSpecialData();

    /**
     * Called to modify the mob's inherited attributes.
     */
    public void adjustEntityAttributes();

    static void loadChat(String localName, ArrayList<ChatComponentText> chatSnark,
            ArrayList<ChatComponentText> chatSuper) {
        int count;

        // Read snark count
        count = Integer.parseInt(StatCollector.translateToLocal(localName + ".snark.count"));
        // Loop through filling up with snark
        for (; count > 0; count--) {
            chatSnark.add(
                    new ChatComponentText(
                            EnumChatFormatting.DARK_RED
                                    + StatCollector.translateToLocal(localName + ".snark." + Integer.toString(count))));
        }

        // Read super count
        count = Integer.parseInt(StatCollector.translateToLocal(localName + ".super.count"));
        // Loop through filling up with snark
        for (; count > 0; count--) {
            chatSuper.add(
                    new ChatComponentText(
                            EnumChatFormatting.BLUE
                                    + StatCollector.translateToLocal(localName + ".super." + Integer.toString(count))));
        }
    }

    /**
     * Called to output a snarky/good job chat message damageSource - what caused the damage. If possible, determine if
     * a player did it to send them a message. If not, try and send a message to all entities in the configurable area.
     * isSnark - Use the Snark or Super chat list
     */
    @SuppressWarnings("unchecked")
    default void sendChatSnark(EntityLiving target, DamageSource damageSource, Random rand,
            ArrayList<ChatComponentText> chat) {
        // Skip generic/falling/etc damage sources
        if (CHAT_ENABLED || damageSource.damageType.matches("generic")
                || damageSource.damageType.matches("fall")
                || damageSource.damageType.matches("cactus")
                || damageSource.damageType.matches("drown")
                || damageSource.damageType.matches("inWall")) {
            return;
        }
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (damageSource.getSourceOfDamage() instanceof EntityClientPlayerMP) {
                return;
            }
        }
        // Ingnore damage caused by FakePlayers like diamond spikes or Killer Joes
        if (damageSource.getSourceOfDamage() instanceof FakePlayer) {
            return;
        }
        // Ignore throwables from dispensers and the like
        if (damageSource.getSourceOfDamage() instanceof EntityThrowable) {
            if (((EntityThrowable) damageSource.getSourceOfDamage()).getThrower() == null) {
                return;
            }
        }
        if (target.getHealth() != target.getMaxHealth()) { // We always display on the first hit
            // Only have a 1 in 16 chance of displaying a message after first shot
            if (rand.nextInt(16) != 0) {
                return;
            }
        }
        ArrayList<EntityPlayer> chatTargets = new ArrayList<EntityPlayer>();
        // Determine if damageSource is a player
        if (damageSource.getEntity() instanceof EntityPlayer) {
            chatTargets.add((EntityPlayer) damageSource.getEntity());
        } else { // Could not find source entity from damageSource, have to just search locally and broadcast
            ArrayList<Entity> nearby = (ArrayList<Entity>) target.worldObj.getEntitiesWithinAABBExcludingEntity(
                    target,
                    target.boundingBox.expand(CHAT_RANGE, CHAT_RANGE, CHAT_RANGE));
            for (Entity nearbyEntity : nearby) {
                if (nearbyEntity instanceof EntityPlayer) {
                    chatTargets.add((EntityPlayer) nearbyEntity);
                }
            }
        }
        // Now output the stuffs;

        int messageId = rand.nextInt(chat.size());

        for (EntityPlayer listeningPlayer : chatTargets) {
            listeningPlayer.addChatMessage(chat.get(messageId));
        }

        return;
    }
}
