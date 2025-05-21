/*
** @author philippe.queinnec@enseeiht.fr
** Inspired by IBM TSpaces exemples.
**
**/

package whiteboard;

import com.ibm.tspaces.*;

public class Whiteboard {

    /** This is the Host where the TupleSpace Server is running. */
    private static String Host = TupleSpace.DEFAULTHOST;
    /** This is the port number for the TupleSpace server. */
    private static int Port = TupleSpace.DEFAULTPORT;

    /*** main **
     ** Run the whiteboard as an application.
     **
     ** @param args - command line arguments
     */
    public static void main(String args[]) {
        if(args.length >= 1)
          Host = args[0]; 
        if(args.length >= 2) {
            try {                               
                Port = Integer.valueOf(args[1]).intValue();
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid Port specification: " + args[1]);
            }
        }
        WhiteboardModel model = new WhiteboardModel();
        WhiteboardView view = new WhiteboardView(model);
        model.setView(view);
        model.start(Host, Port);
        
        /*****
         If we wanted to start out own server we could do the following
              
         private TSServer tss = null;
         private Thread tsServerThread = null;
              
         whiteboard.tss = new TSServer();
         whiteboard.tsServerThread = new Thread(whiteboard.tss);
         whiteboard.tsServerThread.start();
         whiteboard.ts = new TupleSpace("Whiteboard");
        ******/
    }
}

