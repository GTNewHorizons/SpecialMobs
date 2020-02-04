package toast.specialMobs.entity;

import java.util.ArrayList;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import toast.specialMobs.Properties;
import toast.specialMobs.entity.EntityAILavaMonsterAttack;
import toast.specialMobs.entity.ISpecialMob;

public class EntityLavaMonster extends EntityMob implements ISpecialMob {
    /// Handy properties for this class.
    public static final double MAX_HEALTH = Properties.getDouble(Properties.LAVAMONSTER_GENERAL, "lavamonster_health");
    public static final int BASE_ARMOR = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_armor");
    public static final double SPAWN_CHANCE = Properties.getDouble(Properties.LAVAMONSTER_SPAWNING, "lavamonster_spawn_chance");
    public static final boolean ANIMATE_TEXTURE = Properties.getBoolean(Properties.LAVAMONSTER_GENERAL, "lavamonster_animated_texture");
    public static final boolean START_FIRES = Properties.getBoolean(Properties.LAVAMONSTER_GENERAL, "lavamonster_fires");
    public static final boolean BASIC_LOOT = Properties.getBoolean(Properties.LAVAMONSTER_GENERAL, "lavamonster_basic_loot");
    public static final int HEAL_TIME = Properties.getInt(Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_time");
    public static final double HEAL_PERCENT = Properties.getDouble(Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_percent");
    public static final double HEAL_MAX = Properties.getDouble(Properties.LAVAMONSTER_GENERAL, "lavamonster_heal_max");
    
	// Snark is for using the wrong weapon
	public static ArrayList<ChatComponentText> chatSnark = new ArrayList<ChatComponentText>(); 
	// Super is for using the right weapon
	public static ArrayList<ChatComponentText> chatSuper = new ArrayList<ChatComponentText>(); 

    /// Ticks until the next attack phase change.
    public int attackDelay = 0;
    /// True if the texture index is increasing.
    private boolean textureInc = true;
    /// Counter to next texture update.
    private byte textureTicks = 0;
    /// The current texture index.
    private byte textureIndex = 0;

    static {
    	// Load up chat
    	ISpecialMob.loadChat( "entity.SpecialMobs.LavaMonster", chatSnark, chatSuper);
    }
    
    public EntityLavaMonster(World world) {
        super(world);
        this.setSize(0.8F, 2.2F);
        this.isImmuneToFire = true;
        this.experienceValue = 8;
        this.getNavigator().setAvoidsWater(true);
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAILavaMonsterAttack(this, 1.0));
        this.tasks.addTask(2, new EntityAIWander(this, 1.0));
        this.tasks.addTask(3, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(3, new EntityAILookIdle(this));
        this.targetTasks.addTask(0, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    /// Returns true if the new AI should be run.
    @Override
    protected boolean isAIEnabled() {
        return true;
    }

    /// Initializes this entity's attributes.
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(EntityLavaMonster.MAX_HEALTH);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.24);
    }

    /// Used to initialize dataWatcher variables.
    @Override
    protected void entityInit() {
        super.entityInit();
        /// burningState; While this is 1, the lava monster will be rendered on fire.
        this.dataWatcher.addObject(31, Byte.valueOf((byte) 0));
    }

    /// Returns the armor of this entity.
    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + EntityLavaMonster.BASE_ARMOR);
    }

    /// Called every tick while this entity is alive.
    @Override
    public void onLivingUpdate() {
        if (this.worldObj.isRemote) {
            this.updateTexture();
        }
        else {
            this.attackDelay = Math.max(0, this.attackDelay - 1);
            if (this.isWet()) {
                this.attackEntityFrom(DamageSource.drown, 1);
            }
        }
        if (this.rand.nextInt(100) == 0) {
            this.worldObj.spawnParticle("lava", this.posX + (this.rand.nextDouble() - 0.5) * this.width, this.posY + this.rand.nextDouble() * this.height + this.height / 2.0, this.posZ + (this.rand.nextDouble() - 0.5) * this.width, 0.0, 0.0, 0.0);
            this.worldObj.playSoundAtEntity(this, "liquid.lavapop", 0.2F + this.rand.nextFloat() * 0.2F, 0.9F + this.rand.nextFloat() * 0.15F);
        }
        if (this.rand.nextInt(200) == 0) {
            this.worldObj.playSoundAtEntity(this, "liquid.lava", 0.2F + this.rand.nextFloat() * 0.2F, 0.9F + this.rand.nextFloat() * 0.15F);
        }
        // On average, every HEAL_TIME (multiplied by 2 because average) heal the lava monster if it is standing in lava. This way players have to lure it out to do more damage
        if (( this.rand.nextInt(HEAL_TIME*2) == 0) && (this.worldObj.getBlock((int) Math.round(posX), (int) Math.round(posY), (int) Math.round(posZ)).getMaterial() == Material.lava)) {
        	float damage = this.getMaxHealth() - this.getHealth();
        	if( damage != 0) {
	        	// Always try healing at least 1 half-heart
	        	damage = (float) Math.max(damage*HEAL_PERCENT, 1);
	        	// Heal damage or HEAL_MAX/2 hearts, whichever is smaller.
	        	this.heal( (float) Math.min( damage, HEAL_MAX));
        	}
        }
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY);
        int z = MathHelper.floor_double(this.posZ);
        if (START_FIRES && this.worldObj.isAirBlock(x, y, z)) {
            this.worldObj.setBlock(x, y, z, Blocks.fire, 0, 2);
        }
        
        // 2 extra age ticks is added in bright areas, but since this spawns in lava, we want to negate those 2 extra ticks.
        if (this.getBrightness(1.0F) > 0.5F) {
            this.entityAge -= 2;
        }
        super.onLivingUpdate();
    }

    /// Shoots the target with a fireball.
    public void attackEntityWithFireball(EntityLivingBase target) {
        EntitySmallFireball fireball = new EntitySmallFireball(this.worldObj, this, target.posX - this.posX, target.boundingBox.minY + target.height / 2.0F - this.posY - this.height / 2.0F, target.posZ - this.posZ);
        fireball.posY = this.posY + this.height - 0.5;
        this.playSound("mob.ghast.fireball", 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
        this.worldObj.spawnEntityInWorld(fireball);
    }

    /// Called when the entity is attacked.
    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
    	if ( isDamageSourceEffective( damageSource ) ) {
        	// Snowballs and other frost damage sources are super-effective. Only takes 3 hits to kill them
            damage = Math.max(this.getMaxHealth()/2 - 1, damage);
            // Super effective message
            sendChatSnark(this, damageSource, this.rand, chatSuper);
        }
        if (damageSource.isFireDamage()) {
        	// What are you, stupid? message
            sendChatSnark(this, damageSource, this.rand, chatSnark);

        	damage = 0;
        }
        return super.attackEntityFrom(damageSource, damage);
    }

    public boolean isDamageSourceEffective(DamageSource damageSource) {
        if (damageSource != null) {
            if (damageSource.canHarmInCreative())
                return true;
            if ((damageSource.getSourceOfDamage() instanceof EntitySnowball) 
              || Properties.EntityFrostShardClass.isInstance(damageSource.getSourceOfDamage()) 
              || Properties.EntityIceArrow.isInstance(damageSource.getSourceOfDamage())) {
                return true;
            }
            Entity attacker = damageSource.getEntity();
            if (attacker instanceof EntityLivingBase) {
                ItemStack heldItem = ((EntityLivingBase)attacker).getHeldItem();
                if (heldItem != null) {
                    if (Properties.ItemTFIceSword.isInstance(heldItem.getItem()) ) {
                    	return true;
                    }
                }
            }
        }
        return false;
    }    
    /// The id of the item this mob drops. 
    @Override
    protected Item getDropItem() {
        return Items.fire_charge;
    }

    /// Called when this entity is killed.
    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (int i = this.rand.nextInt(3 + looting); i-- > 0;) {
            this.dropItem(Items.coal, 1);
        }
        if (recentlyHit && (this.rand.nextInt(2) == 0 || this.rand.nextInt(1 + looting) > 0)) {
            this.dropItem(Items.fire_charge, 1);
        }
        if (!BASIC_LOOT && recentlyHit && (this.rand.nextInt(15) == 0)) {
        	this.dropItem(Items.lava_bucket, 1);
        }
    }

    /// Called 2.5% of the time when this entity is killed. 20% chance that superRare == 1, otherwise superRare == 0.
    @Override
    protected void dropRareDrop(int superRare) {
    	ItemStack drop;
    	if ((superRare == 0)|| BASIC_LOOT) {
	        drop = new ItemStack(Items.iron_boots);
	        drop.setStackDisplayName("\u00a7cLava Slippers");
	        drop.addEnchantment(Enchantment.fireProtection, 3);
	        this.entityDropItem(drop, 1.0F);
    	} else
    	{
    		switch( this.rand.nextInt(4) ) {
    		case 0:
    			drop = new ItemStack(Items.diamond_boots);
		        drop.setStackDisplayName("\u00a7cFirefighter Boots"); // In honor of the brave men and women who run into danger, not away
		        drop.addEnchantment(Enchantment.fireProtection, 4);
		        drop.addEnchantment(Enchantment.featherFalling, 2);
		        this.entityDropItem(drop, 1.0F);
		        break;
    		case 1:
    			drop = new ItemStack(Items.diamond_leggings);
		        drop.setStackDisplayName("\u00a7cFirefighter Pants");
		        drop.addEnchantment(Enchantment.fireProtection, 4);
		        drop.addEnchantment(Enchantment.blastProtection, 2);
		        this.entityDropItem(drop, 1.0F);
		        break;
    		case 2:
    			drop = new ItemStack(Items.diamond_chestplate);
		        drop.setStackDisplayName("\u00a7cFirefighter Jacket");
		        drop.addEnchantment(Enchantment.fireProtection, 4);
		        drop.addEnchantment(Enchantment.projectileProtection, 2);
		        this.entityDropItem(drop, 1.0F);
		        break;
    		case 3:
    			drop = new ItemStack(Items.diamond_helmet);
		        drop.setStackDisplayName("\u00a7cFirefighter Helmet");
		        drop.addEnchantment(Enchantment.fireProtection, 4);
		        drop.addEnchantment(Enchantment.respiration, 2);
		        this.entityDropItem(drop, 0.0F);
		        break;
    		}
    	}
    }

    /// Returns true if this mob is on fire. Used for rendering.
    @Override
    public boolean isBurning() {
        return this.dataWatcher.getWatchableObjectByte(31) == 1;
    }

    /// Sets this lava monster's burningState variable. Used for rendering.
    public void setBurningState(boolean state) {
        this.dataWatcher.updateObject(31, Byte.valueOf(state ? (byte) 1 : (byte) 0));
    }

    /// Updates this mob's texture animation.
    public void updateTexture() {
        if (!EntityLavaMonster.ANIMATE_TEXTURE || ++this.textureTicks < 2)
            return;
        this.textureTicks = 0;
        this.textureIndex += this.textureInc ? 1 : -1;
        if (this.textureIndex < 0) {
            this.textureIndex = 1;
            this.textureInc = true;
        }
        else if (this.textureIndex > 19) {
            this.textureIndex = 18;
            this.textureInc = false;
        }
    }

    /// Returns the current texture index.
    public int getTextureIndex() {
        return this.textureIndex;
    }

    /// Returns the maximum number of this entity can be spawned in a single chunk.
    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    /// Returns true if this mob is in an eligible location to spawn.
    @Override
    public boolean getCanSpawnHere() {
        return this.rand.nextDouble() < EntityLavaMonster.SPAWN_CHANCE && this.worldObj.checkNoEntityCollision(this.boundingBox) && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty();
    }

    /// Returns this entity's hurt sound or null if it does not have one.
    @Override
    protected String getHurtSound() {
        return "mob.ghast.scream";
    }

    /// Returns this entity's death sound or null if it does not have one.
    @Override
    protected String getDeathSound() {
        return "mob.ghast.death";
    }

    /// Returns the volume of this mob's sounds.
    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    /// Returns the brightness of this entity. Used for rendering.
    public int getEntityBrightnessForRender(float f) {
        return 0xf000f0;
    }

    /// Returns the brightness of this entity. Used for rendering.
    public float getEntityBrightness(float f) {
        return 1.0F;
    }
    
   public SpecialMobData getSpecialData() {
	   return null;
   }

   public void adjustEntityAttributes() {
	   return;
   }
}