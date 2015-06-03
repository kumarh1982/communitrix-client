package fr.pierreqr.communitrix.networking.commands.tx;

public class TXCombatPlayTurn extends TXBase {
  public    String      uuid;
  public    fr.pierreqr.communitrix.networking.Quaternion  rotation;
  public    fr.pierreqr.communitrix.networking.Vector      translation;

  public TXCombatPlayTurn (final String uuid, final com.badlogic.gdx.math.Quaternion rotation, final com.badlogic.gdx.math.Vector3 translation) {
    super         (Tx.CombatPlayTurn);
    this.uuid         = uuid;
    this.rotation     = new fr.pierreqr.communitrix.networking.Quaternion(rotation);
    this.translation  = new fr.pierreqr.communitrix.networking.Vector(translation);
  }
}
