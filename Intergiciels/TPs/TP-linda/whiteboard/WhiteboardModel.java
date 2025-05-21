/*
** @author philippe.queinnec@enseeiht.fr
** Inspired by IBM TSpaces exemples.
**
**/

package whiteboard;

import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import com.ibm.tspaces.*;

/**
 ** The model of the whiteboard.
 **
 ** All of the TSpaces handling is done inside this object.
 **
 ** [ KEY_WHITEBOARD, Command.DRAW, ColorShaped ]
 ** [ KEY_WHITEBOARD, Command.ERASEALL ]
 ** [ KEY_WHITEBOARD, Command.LOCK ]
 ** [ KEY_WHITEBOARD, Command.ROTATE, Integer ]
 **
 ** (where KEY_WHITEBOARD="Whiteboard")
 **
 */
public class WhiteboardModel {

    /** This holds a reference to the current TupleSpace. */
    private TupleSpace TSServer;

    /** The graphic part of the whiteboard. */
    private WhiteboardView view;

    private static final String KEY_WHITEBOARD = "Whiteboard";

    /** The commands that can be sent on the tuple spaces. */
    enum Command { DRAW, ERASEALL, ROTATE, LOCK };

    /** The lines and their respective colors that this client knows about. */
    private Set<ColoredShape> lines = new HashSet<>();

    /** true when exclusive access has been acquired */
    private boolean hasExclusiveAccess = false;

    public WhiteboardModel() { }
    
    public void setView(WhiteboardView view) {
        this.view = view;
    }

    public Set<ColoredShape> getLines() {
        return lines;
    }
    
    /**
     ** TupleSpace Initialization routine.
     **
     ** This will setup to access TupleSpace and register to be informed
     ** when anyone writes a "Whiteboard" tuple to the "Whiteboard"
     ** tuplespace.
     **
     ** It will then get all the current Tuples and paint them.
     **
     ** TupleSpace contains the all of the whiteboard tuples that have been
     ** written up to this time. We want to read in and process all of them.
     ** We use the scan method for this and get back a tuple that is a list
     ** of the tuples.
     */
    public void start(String host, int port) {
        try {
            //--------------------------------------------------
            // Setup to access TupleSpace
            // We access the "Whiteboard" tuplespace on the indicatd host.
            //
            // We then just register for the write event
            //   Whiteboard must implement Callback which will then
            //   be called anytime anyone writes a matching tuple to the tuplespace
            //------------------------------------------------------------
            System.out.println("Access Whiteboard tuplespace at " + host+":"+port);
            TupleSpace.setConnectionTries(1);
            TSServer = new TupleSpace("Whiteboard", host, port);

            // Create template to check if initialisation has been done.
            // If not, create the tuple for exlusive access.
            // FIXME: there is a small race condition here.
            Tuple match = new Tuple(KEY_WHITEBOARD, "Someone is already here");
            if (TSServer.read(match) == null) {
                System.out.println("First one to run: I create the lock.");
                TSServer.write(new Tuple(KEY_WHITEBOARD, "Someone is already here"));
                TSServer.write(new Tuple(KEY_WHITEBOARD, Command.LOCK));
            }

            // Create a template to indicate what we are interested in,
            // and place ourselves on the list to be notified when anyone
            // anywhere writes a matching Tuple to this TupleSpace.
            // **** TODO1 ****

            // Same thing for Erase tuples.
            // **** TODO2 ****

            // Same thing for Rotate tuples.
            // **** TODO5 ****

        } catch(TupleSpaceException tse) {
            tse.printStackTrace();
            System.exit(1);
        }

        // During initialization, we need to read all the current
        // ColoredShapes stored at the TSpaces server. So we issue the
        // scan() request to the tupleSpace using a template. This returns a
        // tuple whose fields are each tuples, one for each tuple that
        // matched the template. The tuple.fields() method returns an
        // enumeration that can be followed to access each individual Tuple.
        // Once the individual Tuple is accessed, the getField(int) method
        // is used to access the ColoredShape from the 2nd field.
        // **** TODO4 ****

        // and redraw the view
        view.redraw();
    }

    /**
     ** This is called when the windowClosing event arrives.
     */
    public void terminate() {
        try {
            if (hasExclusiveAccess)
                releaseExclusiveAccess();
            TupleSpace.cleanup();
            Thread.sleep(1000);
            System.exit(0);
        } catch (Exception tse) {
            System.out.println("term(): " + tse.getMessage());
            tse.printStackTrace();
        }
    }

    /** Global Erase of the whiteboard.
     ** Since we will be informed of this in the callback,
     ** we will let the callback update the set of all lines.
     */
    public void eraseAll() {
        System.out.println("Erase all");
            // Delete all drawing tuples,
            // Tell all clients that we did an erase by writing an erase tuple,
            // and delete this tuple.
            // **** TODO2 ****
    }
    
    /** Rotate all the shapes.
     ** Again, we will be informed of this in the callback,
     ** we will let the callback update the set of all lines.
     */
    public void rotateAll(int degree) {
        System.out.println("Rotate all");
            // Tell all clients to rotate by writing a rotate tuple,
            // and delete this tuple
            // **** TODO5 ****
    }

    /** Request an exclusive access to the whiteboard.
     * Block until it has succeeded.
     */
    public void acquireExclusiveAccess() {
        System.out.println("Acquire exclusive access");
        // **** TODO3 ****
    }

    /** Release the exclusive access. */
    public void releaseExclusiveAccess() {
        System.out.println("Release exclusive access");
        // **** TODO3 ****
    }

    /**
     ** Publish a new shape (line or point) to the tuple space
     */
    public void addShape (ColoredShape shape)
    {
        // Build a new draw tuple for the shape and write it into the tuple space.
        // **** TODO1 **** and **** TODO3 ****
        ts.write(shape);
    }


    /*****************************************************************/

    /**
     ** Process the callback from the server that notifies us when anyone
     ** (including ourself) writes to the Whiteboard TupleSpace.
     **
     ** @param eventName_ the name of the event command that caused this call, that is the
     ** name of the client side command of the thread that registered this call, e.g., in the
     ** case of a read, this would be TupleSpace.READ, **not** TupleSpace.WRITE which is the
     ** corresponding command that caused the actual event.
     ** @param tsName_ the name of the tuple space this command was executed on.
     ** @param sequenceNumber_ the sequenceNumber for this event/command
     ** @param tuple_ the returned tuple or a Tuple with an exception inside
     ** @param isException_ was the command processed normaly or was there an exception
     **
     ** @return true if this is the last call this sequence # should be getting otherwise false
     */
    private class EventDraw implements com.ibm.tspaces.Callback {
        public boolean call(String eventName, String tsName, int sequenceNumber, SuperTuple tuple, boolean isException) {
            if (isException) // assume it's a TupleSpaceException for close
              return true;
            try {
                // check the command from the Tuple that we recieved.
                Command cmd = (Command)(tuple.getField(1).getValue());
                if (cmd != Command.DRAW) {
                    System.out.println("WhiteboardModel.EventDraw : unexpected command " + cmd);
                    return false;
                }
                // Process a drawing request
                // **** TODO1 ****
                // Now go repaint the screen.
                view.redraw();
                // We want to continue recieving requests so we will return false.
                return false;
            } catch(TupleSpaceException tse) {
                System.out.println("WhiteboardModel.EventDraw TupleSpaceException " + tse.getMessage());
                return false;
            }
        }
    }

    private class EventRotate implements com.ibm.tspaces.Callback {
        public boolean call(String eventName, String tsName, int sequenceNumber, SuperTuple tuple, boolean isException) {
            if (isException) // assume it's a TupleSpaceException for close
              return true;
            try {
                // check the command from the Tuple that we recieved.
                Command cmd = (Command)(tuple.getField(1).getValue());
                if (cmd != Command.ROTATE) {
                    System.out.println("WhiteboardModel.EventRotate : unexpected command " + cmd);
                    return false;
                }
                // Process a rotate request
                // **** TODO5 ****
                // Now go repaint the screen.
                view.setClear();
                view.redraw();
                return false;
            } catch(TupleSpaceException tse) {
                System.out.println("WhiteboardModel.EventRotate TupleSpaceException " + tse.getMessage());
                return false;
            }
        }
    }

    class EventEraseAll implements com.ibm.tspaces.Callback {
        public boolean call(String eventName, String tsName, int sequenceNumber, SuperTuple tuple, boolean isException) {
            if (isException)
              return true;
            try {
                Command cmd = (Command)(tuple.getField(1).getValue());
                if (cmd != Command.ERASEALL) {
                    System.out.println("WhiteboardModel::EventEraseAll : unexpected command " + cmd);
                    return false;
                }
                // Process an erase request
                // **** TODO2 ****
                // Now go repaint the screen.
                view.setClear();
                view.redraw();
                return false;
            } catch(TupleSpaceException tse) {
                System.out.println("WhiteboardModel.EventEraseAll TupleSpaceException " + tse.getMessage());
                return false;
            }
        }
    }

}

