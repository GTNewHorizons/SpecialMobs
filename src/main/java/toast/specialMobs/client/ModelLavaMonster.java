package toast.specialMobs.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelLavaMonster extends ModelBase {
    public ModelRenderer head;
    public ModelRenderer jaw;
    public ModelRenderer body;
    public ModelRenderer leg1;
    public ModelRenderer leg2;
    public ModelRenderer leg3;
    public ModelRenderer leg4;

    public ModelLavaMonster() {
        this.textureWidth = 64;
        this.textureHeight = 32;
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-3.5F, -6F, -8F, 7, 6, 10);
        this.head.setRotationPoint(0F, 2F, -3F);
        this.head.setTextureSize(64, 32);
        this.jaw = new ModelRenderer(this, 34, 0);
        this.jaw.addBox(-3F, -1F, -7F, 6, 2, 7);
        this.jaw.setRotationPoint(0F, 2F, -3F);
        this.jaw.setTextureSize(64, 32);
        this.body = new ModelRenderer(this, 32, 9);
        this.body.addBox(-4.5F, -10F, -7F, 9, 16, 7);
        this.body.setRotationPoint(0F, 11F, 3F);
        this.body.setTextureSize(64, 32);
        this.leg1 = new ModelRenderer(this, 0, 18);
        this.leg1.addBox(-2F, -2F, -3F, 4, 10, 4);
        this.leg1.setRotationPoint(-5F, 16F, 5F);
        this.leg1.setTextureSize(64, 32);
        this.leg2 = new ModelRenderer(this, 0, 18);
        this.leg2.addBox(-2F, -2F, -3F, 4, 10, 4);
        this.leg2.setRotationPoint(5F, 16F, 5F);
        this.leg2.setTextureSize(64, 32);
        this.leg2.mirror = true;
        this.leg3 = new ModelRenderer(this, 0, 18);
        this.leg3.addBox(-2F, -2F, -2F, 4, 10, 4);
        this.leg3.setRotationPoint(-5F, 16F, -5F);
        this.leg3.setTextureSize(64, 32);
        this.leg4 = new ModelRenderer(this, 0, 18);
        this.leg4.addBox(-2F, -2F, -2F, 4, 10, 4);
        this.leg4.setRotationPoint(5F, 16F, -5F);
        this.leg4.setTextureSize(64, 32);
        this.leg4.mirror = true;
    }

    @Override
    public void render(Entity entity, float time, float walkSpeed, float util, float yaw, float pitch, float scale) {
        this.head.rotateAngleX = pitch / 57.29578F;
        this.head.rotateAngleY = yaw / 57.29578F;
        this.jaw.rotateAngleX = this.head.rotateAngleX + 0.392699F;
        this.jaw.rotateAngleY = this.head.rotateAngleY;
        this.leg1.rotateAngleX = MathHelper.cos(time * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed;
        this.leg2.rotateAngleX = MathHelper.cos(time * 0.6662F) * 1.4F * walkSpeed;
        this.leg3.rotateAngleX = this.leg2.rotateAngleX;
        this.leg4.rotateAngleX = this.leg1.rotateAngleX;
        this.head.render(scale);
        this.jaw.render(scale);
        this.body.render(scale);
        this.leg1.render(scale);
        this.leg2.render(scale);
        this.leg3.render(scale);
        this.leg4.render(scale);
    }
}