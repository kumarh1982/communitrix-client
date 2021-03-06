package fr.pierreqr.communitrix.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.Protocol;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pierreqr.communitrix.networking.cmd.rx.RXBase;
import fr.pierreqr.communitrix.networking.cmd.rx.RXBase.Type;

public class NetworkingManager implements Runnable {
  public interface NetworkDelegate {
    void onServerMessage (final RXBase baseCmd);
  }
  
  // Constants.
  private final static  String        LogTag                = "Networking";

  // Network related members.
  private final     String            host;
  private final     int               port;
  private           Thread            thread          = null;
  private           NetJavaSocketImpl socket          = null;
  private           InputStream       netInput        = null;
  private           OutputStream      netOutput       = null;
  private           ObjectMapper      mapper          = new ObjectMapper();
  private           StringBuilder     sb              = new StringBuilder(8192);
  private final     Array<NetworkDelegate>
                                      delegates       = new Array<NetworkDelegate>();
  private volatile  boolean           shouldRun       = true;
  
  public NetworkingManager (final String h, final int p, final NetworkDelegate d) {
    // Initialize our members.
    host              = h;
    port              = p;
    delegates.add     (d);
    JsonFactory f     = mapper.getFactory();
    f.configure       (com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    f.configure       (com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
  }
  // Delegation getters / setters.
  public void addDelegate (final NetworkDelegate delegate) {
    if (!delegates.contains(delegate, true))
      delegates.add (delegate);
  }
  public void removeDelegate (final NetworkDelegate delegate) {
    delegates.removeValue(delegate, true);
  }
  
  @Override public void run() {
    boolean   ok        = false;
    // Signal our delegate.
    Gdx.app.postRunnable( new Runnable() { @Override public void run() {
      for (final NetworkDelegate delegate : delegates)
        delegate.onServerMessage(new RXBase(Type.Connecting));
    }});
    
    try {
      SocketHints hints = new SocketHints();
      hints.keepAlive   = true;
      socket            = new NetJavaSocketImpl(Protocol.TCP, host, port, hints);
      ok                = socket.isConnected();
    }
    catch (Exception e) {
      Gdx.app.error     (LogTag, "Failed to connect to server: " + e.getMessage());
    }
    // Socket is connected, run our loop.
    if (ok) {
      String      type  = null;
      netInput          = socket.getInputStream();
      netOutput         = socket.getOutputStream();
      // Signal our delegate.
      Gdx.app.postRunnable( new Runnable() { @Override public void run() {
        for (final NetworkDelegate delegate : delegates)
          delegate.onServerMessage(new RXBase(Type.Connected));
      }});
      // Read forever.
      int         buff  = 0;
      while (shouldRun && socket.isConnected()) {
        try {
          if (( buff = netInput.read() )<0) {
            Gdx.app.log (LogTag, "Disconnected from server.");
            break;
          }
        } catch (IOException e) {
          e.printStackTrace ();
          break;
        }
        switch (buff) {
          case '\r': {
            type          = sb.toString();
            sb.setLength  (0);
            break;
          }
          case '\n': {
            // We just got the type for the next payload.
            if (type!=null && sb.length()>0) {
              try {
                Gdx.app.log(LogTag, type + " -> " + sb.toString());
                final RXBase      cmd     = mapper.readValue(sb.toString(), RXBase.Type.valueOf(type).toTypeReference());
                if (cmd!=null)
                  Gdx.app.postRunnable( new Runnable() { @Override public void run() {
                    for (final NetworkDelegate delegate : delegates)
                        delegate.onServerMessage(cmd);
                  }});
              }
              catch (Exception ex) {
                ex.printStackTrace();
                break;
              }
            }
            sb.setLength      (0);
            type              = null;
            break;
          }
          default:
            sb.append     ((char)buff);
        }
      }
    }
    // Signal our delegate.
    Gdx.app.postRunnable( new Runnable() { @Override public void run() {
      for (final NetworkDelegate delegate : delegates)
        delegate.onServerMessage(new RXBase(Type.Disconnected));
    }});
    // Clean all resources.
    dispose         ();
    if (shouldRun)  start();
  }
  
  // If the networking thread isn't running, start it.
  public void start () {
    synchronized (delegates) {
      shouldRun = true;
      if (thread==null) {
        ( thread = new Thread(this) ).start();
      }
    }
  }
  // Stop the networking thread if it is running.
  public void stop () {
    synchronized (delegates) {
      shouldRun = false;
      if (thread!=null) {
        thread.interrupt  ();
        thread    = null;
      }
    }
  }
  // Send data to the server. TODO: This should be asynchroneous.
  public NetworkingManager send (final fr.pierreqr.communitrix.networking.cmd.tx.TXBase command) {
    synchronized (delegates) {
      // Don't send anything if we scheduled the thread for stopping.
      if (thread!=null && netOutput!=null && shouldRun) {
        // Send data to the server.
        try {
          mapper.writeValue   (netOutput, command);
          netOutput.write     ('\n');
        } catch (IOException e) {
          e.printStackTrace   ();
        }
      }
    }
    return this;
  }
  
  public void dispose () {
    stop            ();
    synchronized (delegates) {
      if (socket!=null) {
        socket.dispose  ();
        socket          = null;
        try { netInput.close  (); }
        catch (IOException e) {}
        try { netOutput.close  (); }
        catch (IOException e) {}
        netInput        = null;
        netOutput       = null;
        thread          = null;
      }
      sb.setLength      (0);
    }
  }
}
