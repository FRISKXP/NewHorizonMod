package newhorizon.contents.bullets;

import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Bullet;
import newhorizon.contents.effects.*;

public class NHTrailBulletType extends BasicBulletType {
	public int trailLength = 12;
	public float trailWidth = -1f;
	public float trailDrawsize = 200f;

	public NHTrailBulletType(float speed, float damage, String bulletSprite){
		super(speed, damage, bulletSprite);
		this.despawnEffect = Fx.none;
    }
    
    @Override
	public void init(){
		super.init();
		drawSize = Math.max(drawSize, 2.5f * trailLength * speed);
		trailDrawsize = Math.max(trailDrawsize, 2.5f * trailLength * speed);
		if(trailWidth < 0)trailWidth = width / 6f;
	}
    
	public NHTrailBulletType(float speed, float damage){
		this(speed, damage, "bullet");
    }

	public NHTrailBulletType(){
		this(1f, 1f, "bullet");
    }
    
    @Override
	public void despawned(Bullet b){
		despawnEffect.at(b.x, b.y, b.rotation(), hitColor);
		Effect.shake(despawnShake, despawnShake, b);
		hit(b);
	}
	
	@Override
	public void init(Bullet b) {
		super.init(b);
		b.data(new EffectTrail(trailLength, trailWidth, trailDrawsize));
		EffectTrail t = (EffectTrail)b.data;
		t.clear();
	}

	@Override
	public void hit(Bullet b) {
		super.hit(b);
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail t = (EffectTrail)b.data;
		t.disappear(trailColor);
	}

	@Override
	public void draw(Bullet b) {
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail t = (EffectTrail)b.data;
		t.draw(trailColor);
		super.draw(b);
	}

	@Override
	public void update(Bullet b) {
		if (!(b.data instanceof EffectTrail))return;
		EffectTrail trail = (EffectTrail)b.data;
		trail.updateDelta(b.x, b.y);
		super.update(b);
	}
}














