package toast.specialMobs.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import toast.specialMobs.Properties;

import java.util.ArrayList;
import java.util.Random;

/**
 * This interface is implemented by all monsters in this mod.
 * It allows for commonly used data to be stored conveniently and for
 * attribute changes to be appropriately applied.
 */
public interface ISpecialMob
{
	public static final int CHAT_ENABLED = Properties.getInt(Properties.GENERAL, "chat_enabled");
	public static final int CHAT_RANGE = Properties.getInt(Properties.GENERAL, "chat_range");
	// Snark is for using the wrong weapon
	public ChatComponentText chatSnark[] = {new ChatComponentText(EnumChatFormatting.DARK_RED + "You might want to get a bigger boat."),
			                                new ChatComponentText(EnumChatFormatting.DARK_RED + "Well that didn't seem to do much."),
			                                new ChatComponentText(EnumChatFormatting.DARK_RED + "What were you thinking?") 
	}; 
	// Super is for using the right weapon
	public ChatComponentText chatSuper[] = {new ChatComponentText(EnumChatFormatting.BLUE + "That was super-effective!"),
        									new ChatComponentText(EnumChatFormatting.BLUE + "Wow, that's going to leave a mark!"),
        									new ChatComponentText(EnumChatFormatting.BLUE + "You are going to brag to all your friends about this!") 
	}; 
    /**
     * @return this mob's special data
     */
    public SpecialMobData getSpecialData();

    /**
     * Called to modify the mob's inherited attributes.
     */
    public void adjustEntityAttributes();
    
    /**
     * Called to output a snarky/good job chat message
     * damageSource - what caused the damage. If possible, determine if a player did it to send them a message.
     *                If not, try and send a message to all entities in the configurable area.
     * isSnark - Use the Snark or Super chat list                	
     */
    default void sendChatSnark(EntityLiving target, DamageSource damageSource, Random rand, boolean isSnark) {
    	if( damageSource.damageType.matches("generic")) { // Skip on generic damage sources
    		return;
    	}
    	if (damageSource.getSourceOfDamage() instanceof EntityClientPlayerMP ) {
    		return;
    	}
    	if( target.getHealth() != target.getMaxHealth()) { // We always display on the first hit
    		//Only have a 1 in 12 chance of displaying a message after first shot
    		if (rand.nextInt(12)!=0) {
    			return;
    		}
    	}
    	ArrayList<EntityPlayer> chatTargets = new ArrayList<EntityPlayer>();
    	//Determine if damageSource is a player
    	if (damageSource.getSourceOfDamage() instanceof EntityPlayer) {
    		chatTargets.add( (EntityPlayer) damageSource.getEntity() );
    	} else { // Could not find source entity from damageSource, have to just search locally and broadcast
    		ArrayList<Entity> nearby = new ArrayList<Entity>( target.worldObj.getEntitiesWithinAABBExcludingEntity(target, target.boundingBox.expand(CHAT_RANGE, CHAT_RANGE, CHAT_RANGE)));
    		for( Entity nearbyEntity : nearby) {
    			if( nearbyEntity instanceof EntityPlayer) {
    				chatTargets.add( (EntityPlayer) nearbyEntity );
    			}
    		}
    	}
    	// Now output the stuffs;
    	ChatComponentText chatOutput[];
    	if (isSnark) {
    		chatOutput = chatSnark;
    	} else {
    		chatOutput = chatSuper;
    	}
    	
    	int messageId = rand.nextInt( chatOutput.length );
    	
    	for( EntityPlayer listeningPlayer : chatTargets ) {
    		listeningPlayer.addChatMessage( chatOutput[messageId]);
    	}
    		
    	return;
    }
}