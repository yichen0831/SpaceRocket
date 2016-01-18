package com.ychstudio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.kotcrab.vis.ui.VisUI;
import com.ychstudio.SpaceRocket;
import com.ychstudio.actors.Actor;
import com.ychstudio.actors.Ground;
import com.ychstudio.actors.Player;
import com.ychstudio.background.Background;
import com.ychstudio.gamesys.ActorBuilder;
import com.ychstudio.gamesys.GM;


public class PlayScreen implements Screen{
    public final float WIDTH = 20f;
    public final float HEIGHT = 30f;
    
    private SpaceRocket game;
    private SpriteBatch batch;
    
    private Stage stage;
    private Label playerSpeedLabel;
    private Label playerPositionLabel;
    private Label playerPauseLabel;
    private Label gameOverLabel;
    
    private float gameOverCountDown = 1.0f;
    
    private FitViewport viewport;
    private OrthographicCamera camera;
    
    private World world;
    private Box2DDebugRenderer box2DDebugRenderer;
    private boolean showBox2DDebugRenderer = true;
    
    private boolean paused;
    private boolean player_paused;
    
    private Array<Actor> actors;
    private Player player;
    
    private Array<ParticleEffect> particleEffects;
    
    private Background background;
    
    public PlayScreen(SpaceRocket game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        
        stage = new Stage();
        playerSpeedLabel = new Label("Speed:", VisUI.getSkin());
        playerSpeedLabel.setPosition(6f, Gdx.graphics.getHeight() - 22f);
        playerPositionLabel = new Label("Pos:", VisUI.getSkin());
        playerPositionLabel.setPosition(6f, Gdx.graphics.getHeight() - 42f);
        playerPauseLabel = new Label("Paused\npress Y to go back to menu", VisUI.getSkin());
        playerPauseLabel.setAlignment(Align.center);
        playerPauseLabel.setPosition((Gdx.graphics.getWidth() - playerPauseLabel.getWidth()) / 2,
                                        (Gdx.graphics.getHeight() - playerPauseLabel.getHeight()) / 2);
        playerPauseLabel.setVisible(false);
        
        gameOverLabel = new Label("Game Over\npress Enter to restart", VisUI.getSkin());
        gameOverLabel.setAlignment(Align.center);
        gameOverLabel.setPosition((Gdx.graphics.getWidth() - gameOverLabel.getWidth()) / 2,
                                    (Gdx.graphics.getHeight() - gameOverLabel.getHeight()) / 2);
        gameOverLabel.setVisible(false);
        
        stage.addActor(playerSpeedLabel);
        stage.addActor(playerPositionLabel);
        stage.addActor(playerPauseLabel);
        stage.addActor(gameOverLabel);
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(WIDTH, HEIGHT, camera);
        camera.zoom = 0.4f;
        camera.translate(WIDTH/2, HEIGHT/2 * camera.zoom);
        world = new World(new Vector2(0, -20f), true);
        box2DDebugRenderer = new Box2DDebugRenderer();
        
        actors = new Array<>();
        
        ActorBuilder.setWorld(world);
        
        player = ActorBuilder.createPlayer(this, WIDTH/2, 2.5f);
        actors.add(player);
        
        Ground ground = ActorBuilder.createGround(WIDTH/2, 1f);
        actors.add(ground);
        
        particleEffects = new Array<>();
        
        background = new Background(batch, WIDTH, HEIGHT);
        
        paused = false;
        player_paused = false;
        
    }
    
    public void update(float delta) {
        world.step(1f / 60.0f, 8, 3);
        
        for (Actor actor : actors) {
            actor.update(delta);
        }
        
        for (int i = particleEffects.size - 1; i >= 0; i--) {
            if (particleEffects.get(i).isComplete()) {
                particleEffects.removeIndex(i);
            }
        }
        
        playerSpeedLabel.setText(String.format("Speed: %.2f", player.getSpeed()));
        playerPositionLabel.setText(String.format("Pos: %.2f, %.2f", player.getPosition().x, player.getPosition().y));
        
        
        if (!player.isPlayerAlive()) {
            gameOverCountDown -= delta;
            if (gameOverCountDown <= 0) {
                gameOverLabel.setVisible(true);
            }
        }
        else {
            gameOverLabel.setVisible(false);
        }
        
        float target_zoom = camera.zoom;
        if (player.getPosition().y < GM.SKY_LINE) {
            target_zoom = 0.4f;
            if (player.getPosition().y < 6.0f) {
                camera.position.y = 6;
            }
            else {
                camera.position.y = player.getPosition().y;
            }
        }
        else {
            target_zoom = MathUtils.clamp(player.getSpeed() / 10f, 0.9f, 1.0f);
            camera.position.y = player.getPosition().y;
        }
        
        camera.zoom = MathUtils.lerp(camera.zoom, target_zoom, 0.1f);
        
        background.update(player.getPosition());
    }
    
    public void inputHandle(float delta) {
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (player.isPlayerAlive()) {
                player_paused = !player_paused;
                playerPauseLabel.setVisible(player_paused);
            }
            else {
                game.backToMenu();
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!player.isPlayerAlive()) {
                player.restart();
                gameOverCountDown = 1.0f;
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            if (player_paused && !paused) {
                game.backToMenu();
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            showBox2DDebugRenderer = !showBox2DDebugRenderer;
        }
        
    }

    @Override
    public void render(float delta) {
        inputHandle(delta);
        
        if (!(paused || player_paused)) {
            update(delta);
        }
        
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        background.render();
        for (Actor actor : actors) {
            actor.render(batch);
        }
        
        for (ParticleEffect effect : particleEffects) {
            effect.draw(batch, delta);
        }
        batch.end();
        
        stage.draw();
        
        if (showBox2DDebugRenderer) {
            box2DDebugRenderer.render(world, camera.combined);
        }
    }
    
    public Array<ParticleEffect> getParticleEffectArray() {
        return particleEffects;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        world.dispose();
        box2DDebugRenderer.dispose();
        batch.dispose();
        stage.dispose();
    }
}
