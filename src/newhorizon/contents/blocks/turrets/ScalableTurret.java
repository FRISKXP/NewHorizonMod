package newhorizon.contents.blocks.turrets;


import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.*;
import arc.*;
import arc.math.*;
import arc.util.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.ui.Styles;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import newhorizon.contents.bullets.NHBullets;
import newhorizon.contents.interfaces.Scalablec;

import newhorizon.contents.data.*;
import newhorizon.NewHorizon;
import newhorizon.contents.interfaces.Upgraderc;

import static newhorizon.contents.data.UpgradeBaseData.*;
import static mindustry.Vars.*;

public class ScalableTurret extends Turret{
	public UpgradeBaseData defaultBaseData = new UpgradeBaseData();
	public UpgradeAmmoData defaultAmmoData = new UpgradeAmmoData();
	
	public float powerUse;
	
	public Color baseColor = Pal.accent;
	//Load Mod Factories
	public ScalableTurret(String name){
		super(name);
		itemCapacity = 60;
		configurable = true;
		hasPower = true;
		hasItems = true;
	}
	
	
	@Override
	public void load(){
		super.load();
		baseRegion = Core.atlas.find("new-horizon-block-" + size);
		defaultAmmoData.load();
		defaultBaseData.load();
	}
	
	@Override
    public void setStats(){
        super.setStats();
		stats.add(Stat.damage, defaultAmmoData.selectAmmo.damage, StatUnit.none);
    }

    @Override
    public void init(){
        consumes.powerCond(powerUse, TurretBuild::isActive);
        super.init();
	}
	
	public class ScalableTurretBuild extends TurretBuild implements Scalablec{
		public UpgradeBaseData baseData = defaultBaseData;
		public UpgradeAmmoData ammoData = defaultAmmoData;

		protected int fromPos = -1;

		public boolean shooting;
		
		Bullet bullet;
        float bulletLife;
		
		@Override public boolean shouldTurn(){return !shooting;}
    
    	@Override
        public void shoot(BulletType ammo){
        	if(ammo.compareTo(NHBullets.none) == 0)return;
            useAmmo();

            tr.trns(rotation, size * tilesize / 2f);
			ammoData.chargeBeginEffect.at(x + tr.x, y + tr.y, rotation);
			ammoData.chargeEffect.at(x + tr.x, y + tr.y, rotation);
            
			if(ammoData.chargeTime > 0)shooting = true;

            Time.run(ammoData.chargeTime, () -> {
            	if(ammoData.burstSpacing > 0.0001f){
					for(int i = 0; i < ammoData.salvos; i++){
						Time.run(ammoData.burstSpacing * i, () -> {
							if(!isValid())return;
							tr.trns(rotation, (size * tilesize / 2f) - recoil, Mathf.range(ammoData.randX) );
							recoil = recoilAmount;
							heat = 2f;
							bullet(ammo, rotation + Mathf.range(ammoData.inaccuracy));
							effects();
						});
					}
				}
				if(!isValid())return;
				shooting = false;
            });
        }
        
        @Override
        protected void effects(){
            Effect fshootEffect = shootEffect == Fx.none ? peekAmmo().shootEffect : shootEffect;
            Effect fsmokeEffect = smokeEffect == Fx.none ? peekAmmo().smokeEffect : smokeEffect;

			fshootEffect.at(x + tr.x, y + tr.y, rotation);
			fsmokeEffect.at(x + tr.x, y + tr.y, rotation);
			getAmmoData().shootSound.at(x + tr.x, y + tr.y, Mathf.random(0.9f, 1.1f));

			if(shootShake > 0){
                Effect.shake(shootShake, shootShake, this);
			}

			recoil = recoilAmount;
		}

        
        @Override
        public BulletType peekAmmo(){
        	return getAmmoData() == null ? NHBullets.none : getAmmoData().selectAmmo == null ? NHBullets.none : getAmmoData().selectAmmo;
		}
		
		@Override
		public BulletType useAmmo(){
			this.items.remove(consumes.getItem().items);
            return peekAmmo();
        }
		
		public boolean hasAmmo(){
			return consValid();
		}
		
		@Override
		public void drawConfigure(){
			Drawf.dashCircle(x, y, range(), baseColor);

			Lines.stroke(1f, getColor());
			Lines.square(x, y, block().size * tilesize / 2.0f + 1.0f);
			Draw.reset();

			drawConnected();
			if(baseData != null && upgraderc() != null)upgraderc().drawLink();
			drawMode();
		}
		
		@Override
		public void updateTile(){
			super.updateTile();
			
			if(!isContiunous())return;
			if(bulletLife > 0 && bullet != null){
				tr.trns(rotation, block.size * tilesize / 2f, 0f);
                bullet.rotation(rotation);
                bullet.set(x + tr.x, y + tr.y);
                bullet.time(0f);
                heat = 1f;
                recoil = recoilAmount;
                bulletLife -= Time.delta / Math.max(efficiency(), 0.00001f);
                if(bulletLife <= 0f){
                    bullet = null;
                }
            }else if(reload > 0){
                Liquid liquid = liquids.current();
                float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

                float used = (cheating() ? maxUsed * Time.delta : Math.min(liquids.get(liquid), maxUsed * Time.delta)) * liquid.heatCapacity * coolantMultiplier;
                reload -= used;
                liquids.remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }

		}
		
		protected float reloadTime(){
			float realReload = ammoData.reloadTime <= 0 ? reloadTime : ammoData.reloadTime;
			return realReload * (1 - Mathf.clamp(baseData.speedMPL * baseData.level, 0, maxReloadReduce) );
		}
		
		@Override
		protected void updateShooting(){
			if(isContiunous() && bulletLife > 0 && bullet != null){
                return;
            }

            if(reload >= reloadTime()){
                BulletType type = peekAmmo();

                shoot(type);

                reload = 0f;
            }else{
                reload += delta() * peekAmmo().reloadMultiplier * baseReloadSpeed();
            }
        }

		@Override
		protected void updateCooling(){
			float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;
			
			Liquid liquid = liquids.current();

			float used = Math.min(Math.min(liquids.get(liquid), maxUsed * Time.delta), Math.max(0, ((reloadTime - reload) / coolantMultiplier) / liquid.heatCapacity)) * baseReloadSpeed();
			reload += used * liquid.heatCapacity * coolantMultiplier;
			liquids.remove(liquid, used);

			if(Mathf.chance(0.06 * used)){
				coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
			}
		}
		
		@Override
		protected void bullet(BulletType type, float angle){
			if(isContiunous()){
				bullet = type.create(tile.build, team, x + tr.x, y + tr.y, angle);
				bulletLife = ammoData.continuousTime;
			}else{
				float lifeScl = type.scaleVelocity ? Mathf.clamp(Mathf.dst(x + tr.x, y + tr.y, targetPos.x, targetPos.y) / type.range(), minRange / type.range(), range / type.range()) : 1f;
				type.create(this, team, x + tr.x, y + tr.y, angle, 1f + Mathf.range(ammoData.velocityInaccuracy), lifeScl);
			}
        }
		
		@Override
		public void resetUpgrade(){
			fromPos = -1;
			baseData = defaultBaseData;
			ammoData = defaultAmmoData;
		}



		@Override public Color getColor(){return baseColor;}

		@Override public boolean isContiunous(){return ammoData.continuousTime > 0;}
		@Override public float handleDamage(float amount) {return amount * (1 - Mathf.clamp(baseData.defenceMPL * baseData.level, 0, maxDamageReduce));}
		
		@Override public boolean isConnected(){return baseData != null && upgraderc() != null;}
		@Override public Upgraderc upgraderc(){
			if(world.build(fromPos) == null){
				fromPos = -1;
				return null;
			}
			if(world.build(fromPos) != null && world.build(fromPos) instanceof Upgraderc)return (Upgraderc)world.build(fromPos);
			return baseData.from;
		}
		
	    @Override public UpgradeBaseData getBaseData(){return baseData;}
		@Override public UpgradeAmmoData getAmmoData(){return ammoData;}
    	
		@Override public void setBaseData(UpgradeBaseData data){
			this.baseData = data;
			this.fromPos = data.from.pos();
		}
		@Override public void setAmmoData(UpgradeAmmoData data){this.ammoData = data;}

		@Override
		public void write(Writes write) {
			write.i(this.fromPos);
		}

		@Override
		public void read(Reads read, byte revision) {
			this.fromPos = read.i();
		}

		@Override
		public void buildConfiguration(Table t) {
			t.table(Tex.button, table -> {
				table.table(cont -> cont.image(ammoData.icon).left()).left().growX();
				table.table(cont -> {
					cont.add("[lightgray]Level: [accent]" + baseData.level + "[]", Styles.techLabel).left().pad(OFFSET).row();
					cont.image().fillX().pad(OFFSET / 2).height(OFFSET / 3).color(Color.lightGray).left().row();
					cont.add("[lightgray]ReloadReduce: [accent]" + getPercent(baseData.speedMPL * baseData.level, 0f, maxReloadReduce) + "%[]").left().row();
					cont.add("[lightgray]DefenceUP: [accent]" + getPercent(baseData.defenceMPL * baseData.level, 0f, maxDamageReduce) + "%[]").left().row();
				}).growX().right().padRight(OFFSET / 3);
			}).grow().padLeft(OFFSET).padRight(OFFSET).row();

			if(baseData != null && upgraderc() != null)upgraderc().buildSwitchAmmoTable(t, true);

		}
	}
}









