package wingman.game;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.Observer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import wingman.GameWorld;
import wingman.modifiers.AbstractGameModifier;
import wingman.modifiers.motions.InputController;
import wingman.modifiers.weapons.RotatableWeapon;
import wingman.modifiers.weapons.RotatablePlaneCanon;
import wingman.modifiers.weapons.AbstractRotatableWeapon;
import wingman.modifiers.weapons.AbstractWeapon;

/**
 * Represent the player's tank.
 */
public class Tank extends PlayerShip {
    private double angle;
    private GameObject collisionObj;
    private boolean wasDead;
    private AbstractRotatableWeapon weapon;
    private List<Integer> shakeList;
    private boolean isPlane;
    private Image tankImg;

    public Tank(Point location, Point speed, Image img, int[] controls, String name) {
        super(location, speed, img, controls, name);
        this.weapon = new RotatableWeapon();
        this.angle = 0;
        this.collisionObj= null;
        this.wasDead = false;
        this.shakeList = new ArrayList<Integer>();
        this.isPlane = false;
        this.tankImg = img;
    }

    @Override
    public void draw(Graphics g, ImageObserver obs) {
        BufferedImage bimg = new BufferedImage(img.getWidth(obs),
                                               img.getHeight(obs),
                                               BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bimg.createGraphics();
        g2d.drawImage(img, 0, 0, obs);
        g2d.dispose();
        AffineTransform tx = AffineTransform.getRotateInstance(
                -this.angle + Math.PI/2,
                img.getWidth(obs) / 2, img.getHeight(obs) / 2);
        AffineTransformOp op = new AffineTransformOp(tx,
                                AffineTransformOp.TYPE_BILINEAR);
        if(respawnCounter<=0) {
            g.drawImage(op.filter(bimg, null),
                        location.x, location.y, observer);
        }
        else if(respawnCounter==80){
            GameWorld.getInstance().addClockObserver(this.motion);
            respawnCounter -=1;
        }
        else if(respawnCounter<80){
            if(respawnCounter%2==0) {
            g.drawImage(op.filter(bimg, null),
                        location.x, location.y, observer);
            }
            respawnCounter -= 1;
        }
        else
            respawnCounter -= 1;
    }

    @Override
    public void setWeapon(AbstractWeapon weapon) {
        this.weapon = (AbstractRotatableWeapon)weapon;
        if (this.weapon instanceof RotatablePlaneCanon) {
            this.transformToPlane();
        }
    }

    @Override
    public void update(int w, int h) {
        int direction = 0;
        int oldX = location.x;
        int oldY = location.y;
        if(isFiring){
            int frame = GameWorld.getInstance().getFrameNumber();
            if(frame>=lastFired+this.weapon.reload){
                fire();
                lastFired= frame;
            }
        }
        // Rotational movement.
        if(this.right == 1){
            if (this.isPlane) {
                this.angle -= 1 * Math.PI / 180;
            }
            else
                this.angle -= 3 * Math.PI / 180;
        }
        else if(this.left == 1){
            if (this.isPlane) {
                this.angle += 1 * Math.PI / 180;
            }
            else
                this.angle += 3 * Math.PI / 180;
        }
        // Translational movement.
        if (this.up == 1) {
            location.y += Math.sin(-this.angle) * speed.y;
            location.x += Math.cos(this.angle) * speed.x;
        }
        else if (this.down == 1) {
            if (!this.isPlane) {
                location.y -= Math.sin(-this.angle) * speed.y;
                location.x -= Math.cos(this.angle) * speed.x;
            }
        }
        if (this.collisionObj != null) {
            if(this.collision(collisionObj)) {
                location.y = oldY;
                location.x = oldX;
                return;
            }
            this.collisionObj = null;
        }
        // Correct any errors in the coordinates.
        this.coordinateCheck(w, h);
    }

    /**
     * Checks if the current coordinates are within the boundary.
     * The coordinates are updated to legal values if they are out of
     * bounds.
     *
     * @param w the width of the area that contains this Tank.
     * @param h the height of the area that contains this Tank.
     */
    private void coordinateCheck(int w, int h) {
        // The tank is past the left boundary.
        if (location.x < 0) {
            location.x = 0;
        }
        // The tank is past the right boundary.
        else if (location.x > (w - width)) {
            location.x = w - width;
        }
        // The tank is past the top boundary.
        if (location.y < 0) {
            location.y = 0;
        }
        // The tank is past the bottom boundary.
        else if (location.y > (h - height)) {
            location.y = h - height;
        }
    }
    
    @Override
    public void collide(GameObject otherObject){
        if (this.isPlane) {
            return;
        }
        this.collisionObj = otherObject;
    }

    private void transformToPlane() {
        if (this.isPlane) {
            return;
        }
        this.isPlane = true;
        health = 1000;
        setImage(GameWorld.getInstance().sprites.get("player3"));
    }

    @Override
    public void collide(Ship otherObject){
        if (this.isPlane) {
            return;
        }
        this.collisionObj = otherObject;
        super.damage(1);
    }

    @Override
    public void collide(Bullet otherObject){
        int bulletStrength = otherObject.getStrength();
        super.damage(bulletStrength);
        this.shakeList.clear();
        while (bulletStrength > 1) {
            this.shakeList.add(bulletStrength);
            this.shakeList.add(-bulletStrength);
            bulletStrength = bulletStrength / 2;
        }
        GameWorld.getInstance().setShake(name, this.shakeList.iterator());
    }

    @Override
    public void fire()
    {
        this.weapon.setAngle(this.angle);
        if(respawnCounter<=0){
            this.weapon.fireWeapon(this);
            GameWorld.getInstance().sound.play("Resources/snd_explosion1.wav");
        }
    }
    
    @Override
    public void die(){
        this.wasDead = true;
        this.isPlane = false;
        super.die();
    }
    
    @Override
    public void reset(){
        super.reset();
        this.weapon = new RotatableWeapon();
        setImage(this.tankImg);
    }
    
    @Override
    public boolean isDead(){
        if (this.wasDead) {
            this.wasDead = false;
            return true;
        }
        return false;
    }
}
