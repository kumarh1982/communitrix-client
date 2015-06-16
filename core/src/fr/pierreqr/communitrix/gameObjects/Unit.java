package fr.pierreqr.communitrix.gameObjects;

import java.util.HashMap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import fr.pierreqr.communitrix.Communitrix;
import fr.pierreqr.communitrix.networking.shared.SHCell;
import fr.pierreqr.communitrix.networking.shared.SHPiece;

public class Unit extends FacetedObject {
  private final   HashMap<Integer,Material>     materials;
  private final   HashMap<Integer,MeshBuilder>  builders;

  public Unit () {
    super    ();
    materials   = new HashMap<Integer, Material>();
    builders    = new HashMap<Integer, MeshBuilder>();
  }
  
  protected void begin (final SHPiece piece) {
    // Cache our game instance.
    final Communitrix ctx = Communitrix.getInstance();
    // Prepare a new array of materials.
    final HashMap<Integer,Material> newMaterials = new HashMap<Integer, Material>();
    HashMap<Integer,MeshBuilder>    newBuilders  = new HashMap<Integer, MeshBuilder>();
    // First, count the unique indices.
    for (final SHCell cell : piece.content) {
      final int index = cell.value;
      if (!newMaterials.containsKey(index)) {
        final Material mat = materials.getOrDefault(
            index,
            new Material(
              ColorAttribute.createDiffuse(
                  0.5f + 0.1f*ctx.rand.nextInt(5),
                  0.5f + 0.1f*ctx.rand.nextInt(5),
                  0.5f + 0.1f*ctx.rand.nextInt(5),
                  1.0f)
            )
          );
        newMaterials.put (cell.value, mat);
      }
      if (!newBuilders.containsKey(index)) {
        final MeshBuilder builder = builders.getOrDefault(index, new MeshBuilder());
        builder.begin             (Usage.Position | Usage.Normal, GL20.GL_TRIANGLES);
        newBuilders.put           (index, builder);
      }
    }
    materials.clear   ();
    materials.putAll  (newMaterials);
    builders.clear    ();
    builders.putAll   (newBuilders);
  }
  protected MeshPartBuilder builderFor (final int x, final int y, final int z, final int index, final int direction) {
    return builders.get(index);
  }
  protected Model end () {
    final Communitrix ctx = Communitrix.getInstance();
    ctx.modelBuilder.begin  ();
    for (int index : builders.keySet()) {
      ctx.modelBuilder.part(
          String.format("node%d", index),
          builders.get(index).end(),
          GL20.GL_TRIANGLES,
          materials.get(index));
    }
    return ctx.modelBuilder.end();
  }
}
