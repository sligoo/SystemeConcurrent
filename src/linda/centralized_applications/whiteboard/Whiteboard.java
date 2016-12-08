/*
** @author philippe.queinnec@enseeiht.fr
** Based on IBM TSpaces exemples.
**
**/

package linda.centralized_applications.whiteboard;

import linda.centralized_applications.Application;
import linda.shm.CentralizedLinda;

import java.awt.*;

/**
 ** This class implements a 'shared' whiteboard to be used with Linda.
 ** All of the Linda related code is WhiteboardPanel.java
 ** 
*/
public class Whiteboard extends Panel implements Application {

    protected static final int WIDTH = 300;
    protected static final int HEIGHT = 350;
    public Frame appFrame;
    private CentralizedLinda linda;
        
    public Whiteboard(CentralizedLinda linda) {
      this.linda = linda;
    }
        
    /*** run **
     ** Run the whiteboard as an application.
     **
     ** @param args - command line arguments
     */
    public void run(String args[]) {
      appFrame = new Frame("Whiteboard");
      appFrame.add("Center", this);
      appFrame.setSize(WIDTH,HEIGHT);

      setLayout(new BorderLayout());
      WhiteboardPanel wp = new WhiteboardPanel(this, this.linda);
      add("Center", wp);

      appFrame.setVisible(true);
    }
}

