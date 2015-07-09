package fr.pierreqr.communitrix.screens.inputControllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;
import fr.pierreqr.communitrix.Constants;
import fr.pierreqr.communitrix.Constants.Key;
import fr.pierreqr.communitrix.gameObjects.GameObject;
import fr.pierreqr.communitrix.gameObjects.Piece;

public class ICLobby extends InputAdapter {
  public interface ICLobbyDelegate {
    void              translateWithinView   (final GameObject obj, final Vector3 axis, final boolean checkCollisions);
    void              rotateWithinView      (final GameObject obj, final Vector3 axis, final int angle, final boolean checkCollisions);
    Piece             getClickableAt        (final int screenX, final int screenY);
    boolean           handleSelection       (final Piece piece);
    boolean           handleZoom            (int amount);
  };
  
  private final       ICLobbyDelegate       delegate;
  public              boolean               alt               = false;
  
  public ICLobby (final ICLobbyDelegate newDelegate) {
    delegate   = newDelegate;
  }
  
  @Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
    return delegate.handleSelection(delegate.getClickableAt(screenX, screenY));
  }
  @Override public boolean scrolled (int amount) {
    return delegate.handleZoom(amount);
  }
  
  public final boolean[] keys = new boolean[Key.values().length];
  public boolean[] updateKeys () {
    boolean newAlt = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT);
    // We just switched to ALT mode.
    if (newAlt && !alt)       alt  = true;
    else if (alt && !newAlt)  alt = false;
    // Read all required keys.
    for (final Key key : Key.values())
      keys[key.ordinal()]  = Gdx.input.isKeyJustPressed(Constants.Keys.get(key));
    return                keys;
  }
}
