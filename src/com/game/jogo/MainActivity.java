package com.game.jogo;

import java.io.Console;
import java.util.Vector;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.IUpdateHandler.IUpdateHandlerMatcher;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.color.Color;

import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class MainActivity extends SimpleBaseGameActivity implements IOnSceneTouchListener {

	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	private static final int TAMANHO = 100;
	public static final float VELOCITY = 200.0f;
	public static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
	public static final int MAX_PONTO = 30;
	final FPSLogger fps = new FPSLogger();

	private Camera camera;	
	private Rectangle base;
	private Rectangle bola;
	private Body bodyBola;
	private Body bodyBase;
	private PhysicsWorld physics;
	private Scene scene;
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private TiledTextureRegion mFaceTextureRegion;
	private Body bodyBall;
	private Vector<Rectangle> pontos;

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.camera = new Camera(0, 0, WIDTH, HEIGHT);
		EngineOptions option = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), this.camera);
		return option;
	}

	@Override
	protected void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 64, 32, TextureOptions.BILINEAR);
        this.mFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "face_circle_tiled.png", 0, 0, 2, 1);
        this.mBitmapTextureAtlas.load();
	}

	@Override
	protected Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(fps);

		this.scene = new Scene();
		this.scene.setBackground(new Background(0.0f, 0.0f, 0.0f));
		this.scene.setOnSceneTouchListener(this);

		this.physics = new PhysicsWorld(new Vector2(0, 0), false);

		final VertexBufferObjectManager vertexBufferObjectManager = this .getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, HEIGHT - 2, WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(WIDTH - 2, 0, 2, HEIGHT, vertexBufferObjectManager);
		final Rectangle shelf = new Rectangle(300, 200, 100, 2, vertexBufferObjectManager);

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.physics, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physics, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physics, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physics, right, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.physics, shelf, BodyType.StaticBody, wallFixtureDef);

		this.scene.attachChild(ground);
		this.scene.attachChild(roof);
		this.scene.attachChild(left);
		this.scene.attachChild(right);

		this.base = new Rectangle((WIDTH - 50), ((HEIGHT / 2) - (TAMANHO / 2)), 10, TAMANHO, this.mEngine.getVertexBufferObjectManager());
		this.base.setColor(Color.WHITE);
		this.bodyBase = PhysicsFactory.createBoxBody(this.physics, this.base, BodyType.StaticBody, FIXTURE_DEF);

		this.scene.attachChild(this.base);
		this.physics.registerPhysicsConnector(new PhysicsConnector(this.base, this.bodyBase, true, true));
		
		final float centerX = (WIDTH - this.mFaceTextureRegion.getWidth()) / 2;
        final float centerY = (HEIGHT - this.mFaceTextureRegion.getHeight()) / 2;
		final Ball ball = new Ball(centerX, centerY, this.mFaceTextureRegion,this.mEngine.getVertexBufferObjectManager());

		this.scene.attachChild(ball);
		
		this.createPontos();

		this.scene.registerUpdateHandler(this.physics);
		
		this.scene.registerUpdateHandler(new IUpdateHandler() {
			
			@Override
			public void reset() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onUpdate(float pSecondsElapsed) {
				// TODO Auto-generated method stub
				if( ball.collidesWith(base) ){
					ball.mudaDirecao();
				}
				for( int x = 0; x < pontos.size(); x++ ){
					if( ball.collidesWith(pontos.get(x)) ){
						ball.mudaDirecao();
						scene.detachChild(pontos.get(x));
						pontos.remove(x);
					}
				}
					
			}
		});
		
		
		return this.scene;
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if (this.physics != null) {
			if (pSceneTouchEvent.isActionMove()) {
				this.bodyBase.setTransform((WIDTH - 50)/32, pSceneTouchEvent.getY()/32, 0);
			}
		}
		return false;
	}

	private void createPontos(){
		this.pontos = new Vector<Rectangle>();
		int x = 10;
		
		for( int i = 0; i < (MAX_PONTO/10); i++ ){
			
			Rectangle rect = new Rectangle(x, 30, 5, 50, this.mEngine.getVertexBufferObjectManager());
			rect.setColor(Color.GREEN);
			Body body = PhysicsFactory.createBoxBody(this.physics, rect, BodyType.StaticBody, FIXTURE_DEF);
			this.scene.attachChild(rect);
			this.physics.registerPhysicsConnector(new PhysicsConnector(rect, body, true,true));
		
			this.pontos.add(rect);
			
			for( int v = 0; v < 10; v++ ){
			
				rect = new Rectangle(x, 30+(55*v), 5, 50, this.mEngine.getVertexBufferObjectManager());
				rect.setColor(Color.GREEN);
				body = PhysicsFactory.createBoxBody(this.physics, rect, BodyType.StaticBody, FIXTURE_DEF);
				this.scene.attachChild(rect);
				this.physics.registerPhysicsConnector(new PhysicsConnector(rect, body, true,true));
			
				this.pontos.add(rect);
			}
			
			x = x + 10;
			
		}
	}
	
	public class Ball extends AnimatedSprite {
		public final PhysicsHandler physics;

	    public Ball(final float pX, final float pY, final TiledTextureRegion pTextureRegion, final VertexBufferObjectManager pVertexBufferObjectManager) {
	            super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
	            this.physics = new PhysicsHandler(this);
	            this.registerUpdateHandler(this.physics);
	            this.physics.setVelocity(MainActivity.VELOCITY, MainActivity.VELOCITY);
	    }

	    @Override
	    protected void onManagedUpdate(final float pSecondsElapsed) {
	            if(this.mX < 0) {
	                    this.physics.setVelocityX(MainActivity.VELOCITY);
	            } else if(this.mX + this.getWidth() > MainActivity.WIDTH) {
	                    this.physics.setVelocityX(-MainActivity.VELOCITY);
	            }

	            if(this.mY < 0) {
	                    this.physics.setVelocityY(MainActivity.VELOCITY);
	            } else if(this.mY + this.getHeight() > MainActivity.HEIGHT ) {
	                    this.physics.setVelocityY(-MainActivity.VELOCITY);
	            }

	            super.onManagedUpdate(pSecondsElapsed);
	    }
	    
	    public void mudaDirecao(){
	    	this.physics.setVelocityX(-MainActivity.VELOCITY);
	    }
		

	}

}
