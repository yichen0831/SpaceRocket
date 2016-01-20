package com.ychstudio.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
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
import com.ychstudio.gamesys.WorldContactListener;


public class PlayScreen implements Screen{
    public final float WIDTH = 20f;
    public final float HEIGHT = 30f;
    
    private SpaceRocket game;
    private SpriteBatch batch;
    
    private Stage stage;
    private Label playerSpeedLabel;
    private Label playerPositionLabel;
    private Label playerHpLabel;
    private Label playerPauseLabel;
    private Label gameOverLabel;
    
    private float gameOverCountDown = 1.0f;
    
    private FitViewport viewport;
    private OrthographicCamera camera;
    
    private World world;
    private Box2DDebugRenderer box2DDebugRenderer;
    private boolean showBox2DDebugRenderer = false;
    
    private boolean paused;
    private boolean player_paused;
    
    private Array<Actor> actors;
    private Player player;
    private Ground ground;
    
    private Array<ParticleEffect> particleEffects;
    
    private Background background;

    private BitmapFont monoFont24;
    
    public PlayScreen(SpaceRocket game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("font/MONOFONT.TTF"));
        FreeTypeFontParameter fontParameter = new FreeTypeFontParameter();
        fontParameter.size = 24;
        fontParameter.magFilter = Texture.TextureFilter.Linear;
        fontParameter.minFilter = Texture.TextureFilter.Linear;

        monoFont24 = fontGenerator.generateFont(fontParameter);
        fontGenerator.dispose();

        Label.LabelStyle labelStyle = new Label.LabelStyle(monoFont24, Color.WHITE);

        stage = new Stage();
        playerSpeedLabel = new Label("Speed:", VisUI.getSkin());
        playerSpeedLabel.setPosition(6f, Gdx.graphics.getHeight() - 22f);
        playerPositionLabel = new Label("Pos:", VisUI.getSkin());
        playerPositionLabel.setPosition(6f, Gdx.graphics.getHeight() - 42f);

        playerHpLabel = new Label("HP:", VisUI.getSkin());
        playerHpLabel.setPosition(6f, Gdx.graphics.getHeight() - 62f);

        playerPauseLabel = new Label("Paused\npress Y to go back to menu", labelStyle);
        playerPauseLabel.setAlignment(Align.center);
        playerPauseLabel.setPosition((Gdx.graphics.getWidth() - playerPauseLabel.getWidth()) / 2,
                                        (Gdx.graphics.getHeight() - playerPauseLabel.getHeight()) / 2);
        playerPauseLabel.setVisible(false);
        
        gameOverLabel = new Label("Game Over\npress Enter to restart", labelStyle);
        gameOverLabel.setAlignment(Align.center);
        gameOverLabel.setPosition((Gdx.graphics.getWidth() - gameOverLabel.getWidth()) / 2,
                                    (Gdx.graphics.getHeight() - gameOverLabel.getHeight()) / 2);
        gameOverLabel.setVisible(false);
        
        stage.addActor(playerSpeedLabel);
        stage.addActor(playerPositionLabel);
        stage.addActor(playerHpLabel);
        stage.addActor(playerPauseLabel);
        stage.addActor(gameOverLabel);
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(WIDTH, HEIGHT, camera);
        camera.zoom = 0.4f;
        camera.translate(WIDTH/2, HEIGHT/2 * camera.zoom);
        
        world = new World(new Vector2(0, -20f), true);
        world.setContactListener(new WorldContactListener());
        box2DDebugRenderer = new Box2DDebugRenderer();
        
        actors = new Array<>();
        particleEffects = new Array<>();

        ActorBuilder.setWorld(world);

        player = ActorBuilder.createPlayer(this, WIDTH/2, 2.5f);
        ground = ActorBuilder.createGround(WIDTH/2, 1f);

        gameRestart();

        background = new Background(batch, WIDTH, HEIGHT);
        
        paused = false;
        player_paused = false;
        
    }
    
    public void update(float delta) {
        world.step(1f / 60.0f, 8, 3);

        player.update(delta);

        for (int i = actors.size - 1; i >=0; i--) {
            actors.get(i).update(delta);
            if (actors.get(i).toBeRemoved) {
                actors.get(i).dispose();
                actors.removeIndex(i);
            }
        }

        for (int i = particleEffects.size - 1; i >= 0; i--) {
            if (particleEffects.get(i).isComplete()) {
                particleEffects.removeIndex(i);
            }
        }
        
        playerSpeedLabel.setText(String.format("Speed: %.2f", player.getSpeed()));
        playerPositionLabel.setText(String.format("Pos: %.2f, %.2f", player.getPosition().x, player.getPosition().y));
        playerHpLabel.setText(String.format("HP: %.1f", player.getHp()));
        
        if (!player.isPlayerAlive()) {
            gameOverCountDown -= delta;
            if (gameOverCountDown <= 0) {
                gameOverLabel.setVisible(true);
            }
        }
        else {
            gameOverLabel.setVisible(false);
        }
        
        float target_zoom;
        float target_y;
        if (player.getPosition().y < GM.SKY_LINE) {
            target_zoom = 0.4f;
            if (player.getPosition().y < 6.0f) {
                target_y = 6;
            }
            else {
                target_y = player.getPosition().y;
            }
        }
        else {
            target_zoom = MathUtils.clamp(player.getSpeed() / 10f, 0.9f, 1.0f);
            target_y = player.getPosition().y + HEIGHT / 4f;
        }

        camera.position.y = MathUtils.lerp(camera.position.y, target_y, 0.1f);
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
                gameRestart();
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
        player.render(batch);
        for (Actor actor : actors) {
            actor.render(batch);
        }
        
        for (ParticleEffect effect : particleEffects) {
            effect.draw(batch, delta);
        }

        ground.render(batch);
        batch.end();
        
        stage.draw();
        
        if (showBox2DDebugRenderer) {
            box2DDebugRenderer.render(world, camera.combined);
        }
    }

    public void gameInit() {
        for (Actor actor : actors) {
            actor.dispose();
        }
        actors.clear();
        particleEffects.clear();
    }

    public void gameRestart() {
        gameInit();
        player.restart();

        gameOverCountDown = 1.0f;
    }
    
    public Array<ParticleEffect> getParticleEffectArray() {
        return particleEffects;
    }
    
    public float getCameraZoom() {
        return camera.zoom;
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
        monoFont24.dispose();
    }
}
