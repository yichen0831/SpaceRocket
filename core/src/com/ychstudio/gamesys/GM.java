package com.ychstudio.gamesys;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public class GM {
    private static GM instance = new GM();
    
    public static final float PPM = 64;
    
    public static final short GROUND_CATEGORY_BITS = 1;
    public static final short PLAYER_CATEGORY_BITS = 2;
    
    
    public static final short GROUND_MASK_BITS = PLAYER_CATEGORY_BITS;
    public static final short PLAYER_MASK_BITS = GROUND_CATEGORY_BITS;

    private AssetManager assetManager;

    public static GM getInstance() {
        return instance;
    }

    public static AssetManager getAssetManager() {
        return instance.assetManager;
    }

    private GM() {
        assetManager = new AssetManager();
        assetManager.load("images/Rock.png", Texture.class);
        assetManager.load("images/Rock32.png", Texture.class);
        assetManager.load("images/SpaceShip.png", Texture.class);
        assetManager.load("images/Flame.png", Texture.class);
        assetManager.finishLoading();
    }
}
