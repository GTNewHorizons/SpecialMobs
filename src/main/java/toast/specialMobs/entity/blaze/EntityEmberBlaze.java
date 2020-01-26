package toast.specialMobs.entity.blaze;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Items;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import toast.specialMobs._SpecialMobs;

public class EntityEmberBlaze extends Entity_SpecialBlaze
{
    public static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
        new ResourceLocation(_SpecialMobs.TEXTURE_PATH + "blaze/ember.png")
    };

    public EntityEmberBlaze(World world) {
        super(world);
        this.getSpecialData().setTextures(EntityEmberBlaze.TEXTURES);
    }

    /// Overridden to modify inherited attribites.
    @Override
    protected void adjustTypeAttributes() {
        this.getSpecialData().addAttribute(SharedMonsterAttributes.maxHealth, 10.0);
        this.setRangedAI(0, 6, 60, 100, 0.0F);
        this.getSpecialData().armor += 10;
    }

    /// Called when this entity is killed.
    @Override
    protected void dropFewItems(boolean hit, int looting) {
        super.dropFewItems(hit, looting);
        if (hit) {
	        for (int i = this.rand.nextInt(2 + looting); i-- > 0;) {
	            this.dropItem(Items.coal, 1);
	        }
        }
    }

    /// Ember's specialty is that it has no fireball attacks, only melee
    /// And those melee attacks bypass armor to directly set you on fire inside-out
    @Override
    public boolean attackEntityAsMob(Entity target) {
        this.onTypeAttack(target);
        return true;
    }
    
    /// Overridden to modify attack effects.
    @Override
	protected void onTypeAttack(Entity target) {
    	if (target instanceof EntityLivingBase) {
    		DamageSource g = new DamageSourceEmber();
            target.attackEntityFrom(g, 4.0F);
    	}
    }

    public static class DamageSourceEmber extends DamageSource {
        public DamageSourceEmber() {
            super("cooked");
            setDamageAllowedInCreativeMode();
            setDamageBypassesArmor();
            setDamageIsAbsolute();
        }

        @Override
        public IChatComponent func_151519_b(EntityLivingBase aTarget) {
            return new ChatComponentText(EnumChatFormatting.RED + aTarget.getCommandSenderName() + EnumChatFormatting.WHITE + " cooked from the inside out");
        }
    }
}