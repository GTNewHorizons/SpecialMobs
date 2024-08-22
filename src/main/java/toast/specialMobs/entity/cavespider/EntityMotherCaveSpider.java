package toast.specialMobs.entity.cavespider;

import static net.minecraft.entity.EntityList.classToIDMapping;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.kuba6000.mobsinfo.api.IMobInfoProvider;
import com.kuba6000.mobsinfo.api.MobDrop;

import cpw.mods.fml.common.Optional;
import toast.specialMobs.EffectHelper;
import toast.specialMobs._SpecialMobs;
import toast.specialMobs.entity.SpecialMobData;

@Optional.Interface(iface = "com.kuba6000.mobsinfo.api.IMobInfoProvider", modid = "mobsinfo")
public class EntityMotherCaveSpider extends Entity_SpecialCaveSpider implements IMobInfoProvider {

    public static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
            new ResourceLocation(_SpecialMobs.TEXTURE_PATH + "cavespider/mother.png"),
            new ResourceLocation(_SpecialMobs.TEXTURE_PATH + "cavespider/mother_eyes.png") };

    /// The number of babies spawned on death.
    private byte babies;

    public EntityMotherCaveSpider(World world) {
        super(world);
        this.getSpecialData().setTextures(EntityMotherCaveSpider.TEXTURES);
        this.experienceValue += 2;
        this.babies = (byte) (3 + this.rand.nextInt(4));
    }

    /// Overridden to modify inherited attributes.
    @Override
    public void adjustTypeAttributes() {
        this.getSpecialData().addAttribute(SharedMonsterAttributes.maxHealth, 16.0);
        this.getSpecialData().addAttribute(SharedMonsterAttributes.attackDamage, 3.0);
        this.getSpecialData().setHealTime(20);
        this.getSpecialData().armor += 6;
        this.getSpecialData().arrowDamage += 1.5F;
    }

    /// Called when this entity is killed.
    @Override
    protected void dropFewItems(boolean hit, int looting) {
        // ALL CHANGES IN HERE MUST BE ALSO MADE IN provideDropsInformation
        super.dropFewItems(hit, looting);
        if (hit && (this.rand.nextInt(3) == 0 || this.rand.nextInt(1 + looting) > 0)) {
            this.entityDropItem(
                    new ItemStack(Items.spawn_egg, 1, (int) classToIDMapping.get(EntityCaveSpider.class)),
                    0.0F);
        }

        if (!this.worldObj.isRemote) {
            EntityBabyCaveSpider baby = null;
            for (int i = this.babies; i-- > 0;) {
                baby = new EntityBabyCaveSpider(this.worldObj);
                baby.copyLocationAndAnglesFrom(this);
                baby.setTarget(this.getEntityToAttack());
                baby.onSpawnWithEgg((IEntityLivingData) null);
                this.worldObj.spawnEntityInWorld(baby);
            }
            if (baby != null) {
                this.worldObj.playSoundAtEntity(baby, "random.pop", 1.0F, 2.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
                baby.spawnExplosionParticle();
            }
        }
    }

    /// Called 2.5% of the time when this entity is killed. 20% chance that superRare == 1, otherwise superRare == 0.
    @Override
    protected void dropRareDrop(int superRare) {
        // ALL CHANGES IN HERE MUST BE ALSO MADE IN provideDropsInformation
        ItemStack itemStack;
        String name;
        if (this.rand.nextBoolean()) {
            Item[] armor = { Items.chainmail_helmet, Items.chainmail_chestplate, Items.chainmail_leggings,
                    Items.chainmail_boots };
            String[] armorNames = { "Helmet", "Chestplate", "Leggings", "Boots" };
            int choice = this.rand.nextInt(armor.length);
            itemStack = new ItemStack(armor[choice]);
            name = armorNames[choice];
        } else {
            Item[] tools = { Items.stone_sword, Items.bow, Items.stone_pickaxe, Items.stone_axe, Items.stone_shovel };
            String[] toolNames = { "Sword", "Bow", "Pickaxe", "Axe", "Shovel" };
            int choice = this.rand.nextInt(tools.length);
            itemStack = new ItemStack(tools[choice]);
            name = toolNames[choice];
        }

        int maxDamage = Math.max(itemStack.getMaxDamage() - 25, 1);
        int damage = itemStack.getMaxDamage() - this.rand.nextInt(this.rand.nextInt(maxDamage) + 1);
        if (damage > maxDamage) {
            damage = maxDamage;
        } else if (damage < 1) {
            damage = 1;
        }
        itemStack.setItemDamage(damage);

        EffectHelper.setItemName(itemStack, "Partially Digested " + name, 0xa);
        EffectHelper.addItemText(itemStack, "\u00a77\u00a7oIt's a bit slimy...");
        EffectHelper.enchantItem(this.rand, itemStack, 30);
        EffectHelper.overrideEnchantment(itemStack, Enchantment.unbreaking, 10);

        this.entityDropItem(itemStack, 0.0F);
    }

    @Optional.Method(modid = "mobsinfo")
    @Override
    public void provideDropsInformation(@Nonnull ArrayList<MobDrop> drops) {
        IMobInfoProvider.provideSuperVanillaDrops(drops, EntityCaveSpider.class);
        drops.add(
                MobDrop.create(new ItemStack(Items.spawn_egg, 1, (int) classToIDMapping.get(EntityCaveSpider.class)))
                        .withChance(0.3333d).withLooting());
        final Item[] armor = { Items.chainmail_helmet, Items.chainmail_chestplate, Items.chainmail_leggings,
                Items.chainmail_boots };
        final String[] armorNames = { "Helmet", "Chestplate", "Leggings", "Boots" };
        final Item[] tools = { Items.stone_sword, Items.bow, Items.stone_pickaxe, Items.stone_axe, Items.stone_shovel };
        final String[] toolNames = { "Sword", "Bow", "Pickaxe", "Axe", "Shovel" };
        double chance = 0.5d / armor.length;
        for (int i = 0; i < armor.length; i++) {
            Item item = armor[i];
            int mindamage = 26;
            int maxdamage = item.getMaxDamage() - 25;
            if (mindamage > maxdamage) mindamage = maxdamage;
            ItemStack stack = new ItemStack(item);

            EffectHelper.setItemName(stack, "Partially Digested " + armorNames[i], 0xa);
            EffectHelper.addItemText(stack, "\u00a77\u00a7oIt's a bit slimy...");

            drops.add(
                    MobDrop.create(stack).withType(MobDrop.DropType.Rare).withChance(0.025d * chance)
                            .withRandomEnchant(30).withRandomDamage(mindamage, maxdamage));
        }
        chance = 0.5d / tools.length;
        for (int i = 0; i < tools.length; i++) {
            Item item = tools[i];
            int mindamage = 26;
            int maxdamage = item.getMaxDamage() - 25;
            if (mindamage > maxdamage) mindamage = maxdamage;
            ItemStack stack = new ItemStack(item);

            EffectHelper.setItemName(stack, "Partially Digested " + toolNames[i], 0xa);
            EffectHelper.addItemText(stack, "\u00a77\u00a7oIt's a bit slimy...");

            drops.add(
                    MobDrop.create(stack).withType(MobDrop.DropType.Rare).withChance(0.025d * chance)
                            .withRandomEnchant(30).withRandomDamage(mindamage, maxdamage));
        }
    }

    /// Saves this entity to NBT.
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagCompound saveTag = SpecialMobData.getSaveLocation(tag);
        saveTag.setByte("Babies", this.babies);
    }

    /// Reads this entity from NBT.
    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NBTTagCompound saveTag = SpecialMobData.getSaveLocation(tag);
        if (saveTag.hasKey("Babies")) {
            this.babies = saveTag.getByte("Babies");
        } else if (tag.hasKey("Babies")) {
            this.babies = tag.getByte("Babies");
        }
    }
}
