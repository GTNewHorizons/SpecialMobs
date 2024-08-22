package toast.specialMobs.entity.witch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.kuba6000.mobsinfo.api.MobDrop;

import cpw.mods.fml.common.Optional;
import toast.specialMobs.EffectHelper;
import toast.specialMobs._SpecialMobs;

public class EntityDominationWitch extends Entity_SpecialWitch {

    public static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
            new ResourceLocation(_SpecialMobs.TEXTURE_PATH + "witch/domination.png") };

    /// Ticks before this entity can use its pull ability.
    public int pullDelay;

    public EntityDominationWitch(World world) {
        super(world);
        this.getSpecialData().setTextures(EntityDominationWitch.TEXTURES);
    }

    /// Override to set the attack AI to use.
    @Override
    protected void initTypeAI() {
        this.setMeleeAI();
    }

    /// Overridden to modify inherited attributes.
    @Override
    public void adjustTypeAttributes() {
        this.getSpecialData().multAttribute(SharedMonsterAttributes.movementSpeed, 0.8);
    }

    public static boolean canAffectMind(EntityLivingBase entity) {
        if (entity.isPotionActive(Potion.weakness)) return true;
        ItemStack helmet = entity.getEquipmentInSlot(4);
        return helmet == null || helmet.stackTagCompound == null
                || !helmet.stackTagCompound.getBoolean("SM|MindProtect");
    }

    /// Called every tick while this entity is alive.
    @Override
    public void onLivingUpdate() {
        if (!this.worldObj.isRemote && this.isEntityAlive()
                && this.pullDelay-- <= 0
                && this.getAttackTarget() != null
                && this.rand.nextInt(20) == 0) {
            EntityLivingBase target = this.getAttackTarget();
            double distanceSq = target.getDistanceSqToEntity(this);

            if (distanceSq > 100.0 && distanceSq < 196.0
                    && EntityDominationWitch.canAffectMind(target)
                    && this.canEntityBeSeen(target)) {
                this.pullDelay = 80;

                double vX = this.posX - target.posX;
                double vY = this.posY - target.posY;
                double vZ = this.posZ - target.posZ;
                double v = Math.sqrt(distanceSq);
                double mult = 0.26;

                target.motionX = vX * mult;
                target.motionY = vY * mult + Math.sqrt(v) * 0.1;
                target.motionZ = vZ * mult;
                target.onGround = false;
                if (target instanceof EntityPlayerMP) {
                    try {
                        ((EntityPlayerMP) target).playerNetServerHandler
                                .sendPacket(new S12PacketEntityVelocity(target));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        super.onLivingUpdate();
    }

    /// Overridden to modify attack effects.
    @SuppressWarnings("unchecked")
    @Override
    protected void onTypeAttack(Entity target) {
        if (target instanceof EntityLivingBase) {
            EntityLivingBase livingTarget = (EntityLivingBase) target;
            PotionEffect stolenEffect = null;
            if (EntityDominationWitch.canAffectMind(livingTarget)) {
                for (PotionEffect effect : (Collection<PotionEffect>) livingTarget.getActivePotionEffects()) {
                    try {
                        // isBadEffect is a private field in Versions prior to 1.8.8
                        Field potionEffectField = FieldUtils.getDeclaredField(Potion.class, "isBadEffect");
                        if (potionEffectField == null) // Field not found, let's try the obfuscated name
                            potionEffectField = FieldUtils.getDeclaredField(Potion.class, "field_76418_K");
                        if (potionEffectField != null) {
                            boolean tIsBadPotionEffect = false;
                            Object tFieldObject = FieldUtils
                                    .readField(potionEffectField, Potion.potionTypes[effect.getPotionID()], true);
                            if (tFieldObject != null) tIsBadPotionEffect = (boolean) tFieldObject;

                            if (tIsBadPotionEffect) {
                                stolenEffect = effect;
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (stolenEffect != null) {
                livingTarget.removePotionEffect(stolenEffect.getPotionID());
                int duration = Math.max(200, stolenEffect.getDuration());
                duration *= 1.3;
                this.addPotionEffect(
                        new PotionEffect(stolenEffect.getPotionID(), duration, stolenEffect.getAmplifier()));
                livingTarget.addPotionEffect(
                        new PotionEffect(Potion.wither.id, 110, Math.max(0, stolenEffect.getAmplifier())));
            } else {
                livingTarget.addPotionEffect(new PotionEffect(Potion.wither.id, 70, 0));
            }
        }
    }

    /// Called when this entity is killed.
    @Override
    protected void dropFewItems(boolean hit, int looting) {
        // ALL CHANGES IN HERE MUST BE ALSO MADE IN provideDropsInformation
        super.dropFewItems(hit, looting);
        for (int i = this.rand.nextInt(3 + looting); i-- > 0;) {
            this.dropItem(Items.experience_bottle, 1);
        }
    }

    /// Called 2.5% of the time when this entity is killed. 20% chance that superRare == 1, otherwise superRare == 0.
    @Override
    protected void dropRareDrop(int superRare) {
        // ALL CHANGES IN HERE MUST BE ALSO MADE IN provideDropsInformation
        int damage;
        if (superRare > 0) {
            damage = 0;
        } else {
            damage = Items.golden_helmet.getMaxDamage();
            damage = (int) (0.6F * damage + 0.3F * this.rand.nextInt(damage));
        }
        ItemStack drop = new ItemStack(Items.golden_helmet, 1, damage);
        EffectHelper.setItemName(drop, "Helmet of Mind Protection", 0xd);
        EffectHelper.addItemText(drop, "\u00a77Protects against");
        EffectHelper.addItemText(drop, "\u00a77domination witches");
        drop.addEnchantment(Enchantment.unbreaking, this.rand.nextInt(3) + 1);
        drop.stackTagCompound.setBoolean("SM|MindProtect", true);
        this.entityDropItem(drop, 0.0F);
    }

    @Optional.Method(modid = "mobsinfo")
    @Override
    public void provideDropsInformation(@Nonnull ArrayList<MobDrop> drops) {
        super.provideDropsInformation(drops);
        drops.add(
                MobDrop.create(new ItemStack(Items.experience_bottle)).withChance(MobDrop.getChanceBasedOnFromTo(0, 2))
                        .withLooting());

        ItemStack drop = new ItemStack(Items.golden_helmet);
        EffectHelper.setItemName(drop, "Helmet of Mind Protection", 0xd);
        EffectHelper.addItemText(drop, "\u00a77Protects against");
        EffectHelper.addItemText(drop, "\u00a77domination witches");
        drop.stackTagCompound.setBoolean("SM|MindProtect", true);
        double chance = 0.025d * 0.3333d;
        int mindamage = (int) (0.6d * Items.golden_helmet.getMaxDamage());
        int maxdamage = (int) (mindamage + 0.3d * (Items.golden_helmet.getMaxDamage() - 1));
        for (int i = 1; i <= 3; i++) {
            ItemStack stack = drop.copy();
            stack.addEnchantment(Enchantment.unbreaking, i);
            drops.add(MobDrop.create(stack).withType(MobDrop.DropType.Rare).withChance(chance * 0.2d));
            drops.add(
                    MobDrop.create(stack.copy()).withType(MobDrop.DropType.Rare).withChance(chance * 0.8d)
                            .withRandomDamage(mindamage, maxdamage));
        }
    }
}
